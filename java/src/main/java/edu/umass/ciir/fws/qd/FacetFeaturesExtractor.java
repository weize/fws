/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.qd;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clist.CandidateListParser;
import edu.umass.ciir.fws.crawl.Document;
import edu.umass.ciir.fws.crawl.QuerySetDocuments;
import edu.umass.ciir.fws.feature.CluewebDocFreqMap;
import edu.umass.ciir.fws.feature.TermFeaturesExtractor;
import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
public class FacetFeaturesExtractor implements Processor<Query> {

    /**
     * Need to extract some basic features for term first. Then combine the term
     * features into their facet features.
     */
    private class TermFeatures {

        String term;
        String[] sites;
        double contentWDF; // document matching weight (for an item/term) in QD.
        double clueIDF;

        private TermFeatures(String term) {
            this.term = term;
        }
    }

    HashMap<String, TermFeatures> termFeatures;
    CluewebDocFreqMap clueDfs;
    QuerySetDocuments querySetDocuments;
    List<Document> docs;
    List<FacetFeatures> facetFeatures;
    Query query;
    long topNum;

    String clistDir;
    String qdFeatureDir;
    double clueCdf;

    public FacetFeaturesExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        clistDir = p.getString("clistDir");
        qdFeatureDir = p.getString("qdFeatureDir");
        String clueDfFile = p.getString("clueDfFile");
        clueCdf = p.getLong("clueCdf");
        topNum = p.getLong("topNum");
        termFeatures = new HashMap<>();
        clueDfs = new CluewebDocFreqMap(clueDfFile);

        loadDocuments(p);

    }

    @Override
    public void process(Query query) throws IOException {
        this.query = query;

        System.err.println(String.format("processing query %s", query.id));

        loadCandidateListsToFacetFeatures();

        initializeTermFeatures();

        loadDocuments();

        extractTermFeatures();

        extractFacetFeatures();

        output();
    }

    private void loadDocuments(Parameters p) throws Exception {
        p.set("loadDocsFromIndex", true);
        querySetDocuments = new QuerySetDocuments(p);
    }

    private void loadCandidateListsToFacetFeatures() throws IOException {
        String clistFileName = Utility.getCandidateListFileName(clistDir, query.id, "clean.clist");
        List<CandidateList> clists = CandidateListParser.loadCandidateList(clistFileName, topNum);
        this.facetFeatures = new ArrayList<>();
        for (CandidateList clist : clists) {
            facetFeatures.add(new FacetFeatures(clist));
        }
    }

    private void initializeTermFeatures() {
        termFeatures.clear();
        for (FacetFeatures clist : facetFeatures) {
            for (String term : clist.items) {
                if (!termFeatures.containsKey(term)) {
                    termFeatures.put(term, new TermFeatures(term));
                }
            }
        }
    }

    private void loadDocuments() {
        docs = querySetDocuments.get(query.id);
        for (Document doc : docs) {
            doc.ngramMap = new HashMap<>();
            TermFeaturesExtractor.buildNgramMapFomText(doc.ngramMap, doc.terms, termFeatures);
        }
    }

    /**
     * Extract wdf, idf and sites for terms.
     */
    private void extractTermFeatures() {
        HashSet<String> sites = new HashSet<String>();
        for (String term : termFeatures.keySet()) {
            TermFeatures termFeature = termFeatures.get(term);
            double clueDf = clueDfs.getDf(term);
            double wdf = 0;
            sites.clear();
            for (Document doc : docs) {
                if (doc.ngramMap.containsKey(term)) {
                    wdf += TermFeaturesExtractor.getDocWeight(doc.rank);
                    sites.add(doc.site);
                }
            }
            termFeature.clueIDF = TermFeaturesExtractor.idf(clueDf, clueCdf);
            termFeature.contentWDF = wdf;
            termFeature.sites = sites.toArray(new String[0]);
        }

    }

    @Override
    public void close() throws IOException {

    }

    private void extractFacetFeatures() {
        ArrayList<String> joinSites = new ArrayList<String>();
        HashSet<String> curSites = new HashSet<String>();

        for (FacetFeatures ff : facetFeatures) {
            // set len 
            int len = ff.items.length;
            ff.setFeature(len, FacetFeatures._len);
            // set sites (join sites of each item/term)
            joinSites.clear();
            boolean initial = true;

            double sdoc = 0;
            double sidf = 0;

            for (String term : ff.items) {
                TermFeatures termFeature = termFeatures.get(term);
                sdoc += termFeature.contentWDF;
                sidf += termFeature.clueIDF;

                if (initial) {
                    joinSites.addAll(Arrays.asList(termFeature.sites));
                    initial = false;
                } else {
                    curSites.clear();
                    curSites.addAll(Arrays.asList(termFeature.sites));
                    // remove site not in curSite
                    int size = joinSites.size();
                    for (int j = 0; j < size; j++) {
                        if (!curSites.contains(joinSites.get(j))) {
                            joinSites.remove(j);
                            j--;
                            size--;
                        }
                    }
                }
            }

            ff.sites = joinSites.toArray(new String[0]);

            sdoc /= (double) len;
            sidf /= (double) len;
            ff.setFeature(sdoc, FacetFeatures._WDF);
            ff.setFeature(sidf, FacetFeatures._cluIDF);
            ff.setFeature(sdoc * sidf, FacetFeatures._qdScore);
            ff.setFeature(TextProcessing.join(ff.sites, "|"), FacetFeatures._sites);
        }
    }

    private void output() throws IOException {
        String fileName = Utility.getQdFacetFeatureFileName(qdFeatureDir, query.id);
        Writer writer = Utility.getWriter(fileName);
        for (FacetFeatures ff : facetFeatures) {
            writer.write(ff.toString());
            writer.write("\n");
        }
        writer.close();
        System.err.println(String.format("Written in %s", fileName));
    }
}
