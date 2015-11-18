/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.feature.TermFeaturesExtractor;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author wkong
 */
public abstract class QdFacetFeatureExtractor {

    /**
     * Need to extract some basic features for term first. Then combine the term
     * features into their facet features.
     */
    private static class TermFeatures {

        String term;
        String[] sites;
        double contentWDF; // document matching weight (for an item/term) in QD.
        double clueIDF;

        private TermFeatures(String term) {
            this.term = term;
        }
    }

    public List<FacetFeatures> extract(List<CandidateList> clists, List<RankedDocument> docs) {
        List<FacetFeatures> facetFeatures = loadCandidateListsToFacetFeatures(clists);
        HashMap<String, TermFeatures> termFeatures = initializeTermFeatures(facetFeatures);
        buildDocumentNgramMap(docs, termFeatures);

        extractTermFeatures(termFeatures, docs);
        extractFacetFeatures(docs, facetFeatures, termFeatures);

        return facetFeatures;
    }

    protected List<FacetFeatures> loadCandidateListsToFacetFeatures(List<CandidateList> clists) {
        List<FacetFeatures> facetFeatures = new ArrayList<>();
        for (CandidateList clist : clists) {
            facetFeatures.add(new FacetFeatures(clist));
        }
        return facetFeatures;
    }

    private HashMap<String, TermFeatures> initializeTermFeatures(List<FacetFeatures> facetFeatures) {
        HashMap<String, TermFeatures> termFeatures = new HashMap<>();
        for (FacetFeatures clist : facetFeatures) {
            for (String term : clist.items) {
                if (!termFeatures.containsKey(term)) {
                    termFeatures.put(term, new TermFeatures(term));
                }
            }
        }
        return termFeatures;
    }

    private void buildDocumentNgramMap(List<RankedDocument> docs, HashMap<String, TermFeatures> termFeatures) {
        for (RankedDocument doc : docs) {
            doc.ngramMap = new HashMap<>();
            TermFeaturesExtractor.buildNgramMapFomText(doc.ngramMap, doc.terms, termFeatures);
        }
    }

    /**
     * Extract wdf, idf and sites for terms.
     */
    private void extractTermFeatures(HashMap<String, TermFeatures> termFeatures, List<RankedDocument> docs) {
        HashSet<String> sites = new HashSet<>();
        for (String term : termFeatures.keySet()) {
            TermFeatures termFeature = termFeatures.get(term);
            double clueDf = getDf(term);
            double wdf = 0;
            sites.clear();
            for (RankedDocument doc : docs) {
                if (doc.ngramMap.containsKey(term)) {
                    wdf += TermFeaturesExtractor.getDocWeight(doc.rank);
                    sites.add(doc.site);
                }
            }
            termFeature.clueIDF = TermFeaturesExtractor.idf(clueDf, getCdf());
            termFeature.contentWDF = wdf;
            termFeature.sites = sites.toArray(new String[0]);
        }

    }

    private void extractFacetFeatures(List<RankedDocument> docs, List<FacetFeatures> facetFeatures, HashMap<String, TermFeatures> termFeatures) {
        ArrayList<String> joinSites = new ArrayList<>();
        HashSet<String> curSites = new HashSet<>();

        HashMap<Long, RankedDocument> docMap = new HashMap<>();
        for (RankedDocument doc : docs) {
            docMap.put(doc.rank, doc);
        }

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

            // a hack to deal with a bug:
            // for text candidate lists, the list item may not be found
            // in the original document, due to the difference in tokenization
            // of stanford parsing, and Galago.
            String originalSite = docMap.get(ff.docRank).site;
            joinSites.add(originalSite);

            ff.sites = joinSites.toArray(new String[0]);

            sdoc /= (double) len;
            sidf /= (double) len;
            ff.setFeature(sdoc, FacetFeatures._WDF);
            ff.setFeature(sidf, FacetFeatures._cluIDF);
            ff.setFeature(sdoc * sidf, FacetFeatures._qdScore);
            ff.setFeature(FacetFeatures.joinSitesToString(ff.sites), FacetFeatures._sites);
        }
    }

    /**
     * document frequency
     * @param term
     * @return 
     */
    protected abstract double getDf(String term);

    /**
     * collection document frequency (the total number of documents in the collection)
     * @return 
     */
    protected abstract double getCdf();
}
