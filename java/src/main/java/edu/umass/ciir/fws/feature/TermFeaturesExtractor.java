/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.crawl.Document;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.crawl.*;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Extract term features for each query.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
public class TermFeaturesExtractor implements Processor<TfQuery> {

    final static double LOG2BASE = Math.log(2);
    final static int MAX_TERM_SIZE = CandidateList.MAX_TERM_SIZE; // used to build map for ngram->count
    String clistDir;
    String featureDir;

    QuerySetResults querySetResults;
    List<CandidateList> clists;
    List<Document> docs;
    BufferedWriter writer;
    Logger logger;
    long topNum; // top number of documents used.
    String rankedListFile;
    String docDir;

    TfQuery query;
    TreeMap<String, TermFeatures> termFeatures;
    CluewebDocFreqMap clueDfs; // clue web document frequency
    CandidateListDocFreqMap clistDfs; // Candidate list document frequency in a gloabl candidate list set.

    public TermFeaturesExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        clistDir = p.getString("clistDir");
        featureDir = p.getString("featureDir");
        String clueDfFile = p.getString("clueDfFile");
        String clistDfFile = p.getString("clistDfFile");
        String clistDfMetaFile = p.getString("clistDfMetaFile");
        double clueCdf = p.getLong("clueCdf");
        topNum = p.getLong("topNum");
        rankedListFile = p.getString("rankedListFile");
        docDir = p.getString("docDir");

        termFeatures = new TreeMap<>();
        clueDfs = new CluewebDocFreqMap(new File(clueDfFile), clueCdf); // load clueWebDocFreqs
        clistDfs = new CandidateListDocFreqMap(new File(clistDfFile), new File(clistDfMetaFile));

        loadQuerySetResults();

        logger = Logger.getLogger(TermFeaturesExtractor.class.toString());
    }

    @Override
    public void process(TfQuery query) throws IOException {
        this.query = query;

        System.err.println(String.format("processing query %s", query.id));
        loadCandidateLists();
        initializeTermFeatures();
        loadDocuments();

        extractTermLength(); // feature: length of the feature term

        // features based on documents
        extractDocFeaturesInContentField();
        extractDocFeaturesInTitleField();

        // extrac list features based on different set of candidates lists
        for (String type : CandidateList.clistTypes) {
            extractListFeatures(type);
        }

        extractClueWebIDF();
        extractCandidateListIDF();

        output();

    }

    @Override
    public void close() throws IOException {

    }

    private void loadCandidateLists() throws IOException {
        File clistFile = new File(Utility.getCandidateListFileName(clistDir, query.id, "clean.clist"));
        clists = CandidateList.loadCandidateLists(clistFile, topNum);
    }

    /**
     * Term feature initialization. Load all facet terms in the candidate list
     * set.
     */
    private void initializeTermFeatures() {
        termFeatures.clear();
        for (CandidateList clist : clists) {
            for (String term : clist.items) {
                if (!termFeatures.containsKey(term)) {
                    termFeatures.put(term, new TermFeatures(term));
                }
            }
        }
    }

    private void loadQuerySetResults() throws Exception {
        querySetResults = new QuerySetResults(rankedListFile, topNum);
    }

    private void extractDocFeaturesInContentField() {
        for (Document doc : docs) {
            doc.ngramMap = new HashMap<>();
            buildNgramMapFomText(doc.ngramMap, doc.terms, termFeatures);
        }

        extractTfDfSfFromNgramMap(TermFeatures._contentTf, TermFeatures._contentDf,
                TermFeatures._contentSf, TermFeatures._contentWDf);
    }

    private void extractDocFeaturesInTitleField() {
        for (Document doc : docs) {
            doc.ngramMap = new HashMap<>();
            List<String> terms = Arrays.asList(doc.title.split("\\s+"));
            buildNgramMapFomText(doc.ngramMap, terms, termFeatures);
        }

        extractTfDfSfFromNgramMap(TermFeatures._titleTf, TermFeatures._titleDf,
                TermFeatures._titleSf, -1);
    }

    /**
     * build ngram->tf map for a unit (document content, title, ) to make
     * counting more efficient. Only keep ngrams in map.
     *
     * @param ngramMap
     * @param terms
     * @param map
     */
    public static void buildNgramMapFomText(HashMap<String, Integer> ngramMap, List<String> terms, Map map) {
        ngramMap.clear();
        for (int len = 1; len <= MAX_TERM_SIZE; len++) {
            for (int i = 0; i + len <= terms.size(); i++) {
                String ngram = TextProcessing.join(terms.subList(i, i + len), " ");
                if (map.containsKey(ngram)) { // only keeps terms in the map
                    if (ngramMap.containsKey(ngram)) {
                        int freq = ngramMap.get(ngram) + 1;
                        ngramMap.put(ngram, freq);

                    } else {
                        ngramMap.put(ngram, 1);
                    }

                }
            }
        }
    }

    /**
     * Extract tf-based features from ngramMap in document.
     *
     * @param tfIndex
     * @param dfIndex
     * @param sfIndex
     * @param wdfIndex if wdfIndex == -1, wdf will not be set in the feature
     * set.
     */
    private void extractTfDfSfFromNgramMap(int tfIndex, int dfIndex, int sfIndex, int wdfIndex) {
        HashSet<String> sites = new HashSet<>();

        for (String t : termFeatures.keySet()) {
            int tf = 0;
            int df = 0;
            double wdf = 0;
            sites.clear();
            for (Document doc : docs) {
                if (doc.ngramMap.containsKey(t)) {
                    int count = doc.ngramMap.get(t);
                    tf += count;
                    df++;
                    wdf += getDocWeight(doc.rank);
                    sites.add(doc.site);
                }

            }

            int sf = sites.size();
            double tfLog = log(tf);
            double dfLog = log(df);
            double sfLog = log(sf);

            TermFeatures termFeature = termFeatures.get(t);
            termFeature.setFeature(tfLog, tfIndex);
            termFeature.setFeature(dfLog, dfIndex);
            termFeature.setFeature(sfLog, sfIndex);
            if (wdfIndex != -1) {
                termFeature.setFeature(wdf, wdfIndex);
            }
        }

    }

    private double log(double a) {
        return Math.log(a + 1) / LOG2BASE;
    }

    public static double getDocWeight(long rank) {
        return 1.0 / Math.sqrt((double) rank);
    }

    private void extractTermLength() {
        for (String term : termFeatures.keySet()) {
            int len = TextProcessing.countWords(term);
            termFeatures.get(term).setFeature(len, TermFeatures._len);
        }
    }

    private void loadDocuments() throws IOException {
        docs = Document.loadDocumentsFromFiles(querySetResults.get(query.id), docDir, query.id);
    }

    /**
     * *
     *
     * @param clistListType the set of candidate lists used for extract
     * feature.（"all", "ul", "ol", "select", "tr", "td", "tx"）
     */
    private void extractListFeatures(String clistListType) {
        // collect lists for each doc
        HashMap<Long, ArrayList<CandidateList>> docidListsMap = new HashMap<>();
        for (CandidateList clist : this.clists) {
            if (clistListType.equals("all") || clistListType.equals(clist.listType)) {
                ArrayList<CandidateList> docLists;
                if (docidListsMap.containsKey(clist.docRank)) {
                    docLists = docidListsMap.get(clist.docRank);
                } else {
                    docLists = new ArrayList<>();
                }
                docLists.add(clist);
                docidListsMap.put(clist.docRank, docLists);
            }
        }

        // build a term-> count map for each doc
        for (Document doc : docs) {
            HashMap<String, Integer> ngramMap = doc.ngramMap; // renaming for convience
            ngramMap.clear();
            if (docidListsMap.containsKey(doc.rank)) {
                for (CandidateList list : docidListsMap.get(doc.rank)) {
                    for (String ngram : list.items) {
                        // only keeps facet terms
                        if (termFeatures.containsKey(ngram)) {
                            if (ngramMap.containsKey(ngram)) {
                                int freq = ngramMap.get(ngram) + 1;
                                ngramMap.put(ngram, freq);

                            } else {
                                ngramMap.put(ngram, 1);
                            }
                        }

                    }
                }
            }
        }

        int[] indices = TermFeatures.getIndicesForListTFs(clistListType);
        extractTfDfSfFromNgramMap(indices[0], indices[1], indices[2], -1);

    }

    private void extractClueWebIDF() {
        for (String term : termFeatures.keySet()) {
            TermFeatures termFeature = termFeatures.get(term);
            double clueDf = clueDfs.getDf(term);
            double contentTF = (Double) termFeature.getFeature(TermFeatures._contentTf);
            double clueIdf = idf(clueDf, clueDfs.clueCdf);
            double contentTFClueIDF = contentTF * clueIdf;
            termFeature.setFeature(clueIdf, TermFeatures._clueIDF);
            termFeature.setFeature(contentTFClueIDF, TermFeatures._contentTFClueIDF);
        }
    }

    public static double idf(double df, double colSize) {
        return Math.log((colSize - df + 0.5) / (df + 0.5)) / LOG2BASE;
    }

    private void extractCandidateListIDF() {
        for (String term : termFeatures.keySet()) {
            TermFeatures termFeature = termFeatures.get(term);

            double listDf = clistDfs.getDf(term, CandidateListDocFreqMap._df);
            double listCdf = clistDfs.getCdf(CandidateListDocFreqMap._df); // collection freq
            double listIdf = idf(listDf, listCdf);

            termFeature.setFeature(listIdf, TermFeatures._listIDF);

            double listTF = (Double) termFeature.getFeature(TermFeatures._listTf);
            double listTfListIDf = listTF * listIdf;
            termFeature.setFeature(listTfListIDf, TermFeatures._listTFListIDF);
        }

    }

    private void output() throws IOException {
        String fileName = Utility.getTermFeatureFileName(featureDir, query.id);
        Writer writer = Utility.getWriter(fileName);
        for (String term : termFeatures.keySet()) {
            writer.write(termFeatures.get(term).toString());
            writer.write("\n");
        }
        writer.close();
        System.err.println(String.format("Written in %s", fileName));
    }

    /**
     * Represents facet term features.
     *
     * @author wkong
     */
    public static class TermFeatures {

        final static String version = "8";

        String term;
        Object[] features;

        // index of features
        public static final int _len = 0; // number of tokens 

        public static final int _listTf = 1; // tf
        public static final int _listDf = 2; // docs contains the term in candidate list
        public static final int _listSf = 3; // site freq
        // for <ul> pattern
        public static final int _listUlTf = 4;
        public static final int _listUlDf = 5;
        public static final int _listUlSf = 6;
        // for <ol> pattern
        public static final int _listOlTf = 7;
        public static final int _listOlDf = 8;
        public static final int _listOlSf = 9;
        // for <select> pattern
        public static final int _listSlTf = 10;
        public static final int _listSlDf = 11;
        public static final int _listSlSf = 12;
        // for <tr> pattern
        public static final int _listTrTf = 13;
        public static final int _listTrDf = 14;
        public static final int _listTrSf = 15;
        // for <td> pattern
        public static final int _listTdTf = 16;
        public static final int _listTdDf = 17;
        public static final int _listTdSf = 18;
        // for text pattern
        public static final int _listTxTf = 19;
        public static final int _listTxDf = 20;
        public static final int _listTxSf = 21;
        // title
        public static final int _titleTf = 22;
        public static final int _titleDf = 23;
        public static final int _titleSf = 24;
        // content
        public static final int _contentTf = 25;
        public static final int _contentDf = 26;
        public static final int _contentWDf = 27;
        public static final int _contentSf = 28;
        // idf
        public static final int _clueIDF = 29;
        public static final int _listIDF = 30;
        // tf.idf
        public static final int _contentTFClueIDF = 31;
        public static final int _listTFListIDF = 32;
        public static final int size = 33;

        public static int[] getIndicesForListTFs(String clistListType) {
            if (clistListType.equals("all")) {
                return new int[]{_listTf, _listDf, _listSf};
            } else if (clistListType.equals("ul")) {
                return new int[]{_listUlTf, _listUlDf, _listUlSf};
            } else if (clistListType.equals("ol")) {
                return new int[]{_listOlTf, _listOlDf, _listOlSf};
            } else if (clistListType.equals("select")) {
                return new int[]{_listSlTf, _listSlDf, _listSlSf};
            } else if (clistListType.equals("tr")) {
                return new int[]{_listTrTf, _listTrDf, _listTrSf};
            } else if (clistListType.equals("td")) {
                return new int[]{_listTdTf, _listTdDf, _listTdSf};
            } else if (clistListType.equals("tx")) {
                return new int[]{_listTxTf, _listTxDf, _listTxSf};
            }
            return null;
        }

        public TermFeatures(String term) {
            this.features = new Object[size];
            this.term = term;
        }

        public void setFeature(Object value, int idx) {
            this.features[idx] = value;
        }

        public Object getFeature(int idx) {
            return this.features[idx];
        }

        @Override
        public String toString() {
            return term
                    + "\t"
                    + TextProcessing.join(features, "\t");
        }
    }
}
