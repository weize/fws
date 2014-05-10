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
@InputClass(className = "edu.umass.ciir.fws.types.Query")
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
    double clueCdf;

    // Candidate list document frequency in a gloabl candidate list set.
    // There are 6 types of "df" here.
    HashMap<String, long[]> clistDfs;
    long[] clistCdfs; // number of "document" in the gloabl candidate list set.

    public TermFeaturesExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        clistDir = p.getString("clistDir");
        featureDir = p.getString("featureDir");
        String clueDfFile = p.getString("clueDfFile");
        String clistDfFile = p.getString("clistDfFile");
        clueCdf = p.getLong("clueCdf");
        topNum = p.getLong("topNum");
        rankedListFile = p.getString("rankedListFile");
        docDir = p.getString("docDir");

        termFeatures = new TreeMap<>();
        clueDfs = new CluewebDocFreqMap(new File(clueDfFile)); // load clueWebDocFreqs
        clistDfs = new HashMap<>();

        loadQuerySetResults();
        loadCandidateListDocFreqs(clistDfFile);

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

        // extrac list features based on all candidates lists
        extractListFeatures(false);
        // extract list features based only on html candidate lists
        extractListFeatures(true);

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

    private void loadCandidateListDocFreqs(String fileName) throws IOException {
        clistCdfs = new long[6];
        clistDfs.clear();

        BufferedReader reader = Utility.getReader(fileName);
        String line;
        reader.readLine(); // read header
        line = reader.readLine(); // read col freqs
        String[] elems = line.split("\t");
        for (int i = 1; i < elems.length; i++) {
            this.clistCdfs[i - 1] = Integer.parseInt(elems[i]);
        }

        while ((line = reader.readLine()) != null) {
            String[] elems2 = line.split("\t");
            String term = elems2[0];
            long[] curDFs = new long[6];
            for (int i = 1; i < elems2.length; i++) {
                curDFs[i - 1] = Integer.parseInt(elems2[i]);
            }
            clistDfs.put(term, curDFs);
        }
        reader.close();
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
     *
     * @param filterTextCandidateList whether filter out candidate lists
     * extracted based on textual patterns.
     */
    private void extractListFeatures(boolean filterTextCandidateList) {
        // collect lists for each doc
        HashMap<Long, ArrayList<CandidateList>> docidListsMap = new HashMap<>();
        for (CandidateList clist : this.clists) {
            // filter text candidate lists if specified
            if (filterTextCandidateList && clist.isTextType()) {
                continue;
            }

            ArrayList<CandidateList> docLists;
            if (docidListsMap.containsKey(clist.docRank)) {
                docLists = docidListsMap.get(clist.docRank);
            } else {
                docLists = new ArrayList<>();
            }
            docLists.add(clist);
            docidListsMap.put(clist.docRank, docLists);
        }

        // build a term-> count map for each doc
        for (Document doc : docs) {
            HashMap<String, Integer> ngramMap = doc.ngramMap; // just renaming for convience
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

        if (filterTextCandidateList) {
            extractTfDfSfFromNgramMap(
                    TermFeatures._listHlTf, TermFeatures._listHlDf, TermFeatures._listHlSf, -1);
        } else {
            extractTfDfSfFromNgramMap(
                    TermFeatures._listTf, TermFeatures._listDf, TermFeatures._listSf, -1);
        }
    }

    private void extractClueWebIDF() {
        for (String term : termFeatures.keySet()) {
            TermFeatures termFeature = termFeatures.get(term);
            double clueDf = clueDfs.getDf(term);
            double contentTF = (Double) termFeature.getFeature(TermFeatures._contentTf);
            double clueIdf = idf(clueDf, clueCdf);
            double contentTFClueIDF = contentTF * clueIdf;
            termFeature.setFeature(clueIdf, TermFeatures._clueIDF);
            termFeature.setFeature(contentTFClueIDF, TermFeatures._contentTFClueIDF);
        }
    }

    public static double idf(double df, double colSize) {
        return Math.log((colSize - df + 0.5) / (df + 0.5)) / LOG2BASE;
    }

    final static int _listDFQueryIdx = 0;
    final static int _listDFQueryHlIdx = 1;
    final static int _listDFDocIdx = 2;
    final static int _listDFDocHlIdx = 3;
    final static int _listDFListIdx = 4;
    final static int _listDFListHlIdx = 5;

    private void extractCandidateListIDF() {
        long[] ones = new long[6];
        for (int i = 0; i < ones.length; i++) {
            ones[i] = 1;
        }

        for (String term : termFeatures.keySet()) {
            TermFeatures termFeature = termFeatures.get(term);

            // assume the frequency is 1 if the term is not found in the candidate list document frequency file.
            long[] dfs = clistDfs.containsKey(term) ? this.clistDfs.get(term) : ones;

            double listQueryIDF = idf(dfs[_listDFQueryIdx], clistCdfs[_listDFQueryIdx]);
            double listQueryHlIDF = idf(dfs[_listDFQueryHlIdx], clistCdfs[_listDFQueryHlIdx]);
            double listDocIDF = idf(dfs[_listDFDocIdx], clistCdfs[_listDFDocIdx]);
            double listDocHlIDF = idf(dfs[_listDFDocHlIdx], clistCdfs[_listDFDocHlIdx]);
            double listListIDF = idf(dfs[_listDFListIdx], clistCdfs[_listDFListIdx]);
            double listListHlIDF = idf(dfs[_listDFListHlIdx], clistCdfs[_listDFListHlIdx]);

            termFeature.setFeature(listQueryIDF, TermFeatures._listQueryIDF);
            termFeature.setFeature(listQueryHlIDF, TermFeatures._listQueryHlIDF);

            termFeature.setFeature(listDocIDF, TermFeatures._listDocIDF);
            termFeature.setFeature(listDocHlIDF, TermFeatures._listDocHlIDF);

            termFeature.setFeature(listListIDF, TermFeatures._listListIDF);
            termFeature.setFeature(listListHlIDF, TermFeatures._listListHlIDF);

            double listHlTF = (Double) termFeature.getFeature(TermFeatures._listHlTf);
            double listTF = (Double) termFeature.getFeature(TermFeatures._listTf);

            double listHlTFListQueryHlIDF = listHlTF * listQueryHlIDF;
            double listHlTFListDocHlIDF = listHlTF * listDocHlIDF;
            double listHlTFListListHlIDF = listHlTF * listListHlIDF;

            double listTFListQueryIDF = listTF * listQueryIDF;
            double listTFListDocIDF = listTF * listDocIDF;
            double listTFListListIDF = listTF * listListIDF;

            termFeature.setFeature(listHlTFListQueryHlIDF, TermFeatures._listHlTFListQueryHlIDF);
            termFeature.setFeature(listHlTFListDocHlIDF, TermFeatures._listHlTFListDocHlIDF);
            termFeature.setFeature(listHlTFListListHlIDF, TermFeatures._listHlTFListListHlIDF);

            termFeature.setFeature(listTFListQueryIDF, TermFeatures._listTFListQueryIDF);
            termFeature.setFeature(listTFListDocIDF, TermFeatures._listTFListDocIDF);
            termFeature.setFeature(listTFListListIDF, TermFeatures._listTFListListIDF);

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
}
