/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clustering.gm.GmIndependentClusterer;
import edu.umass.ciir.fws.clustering.gm.GmJointClusterer;
import edu.umass.ciir.fws.clustering.gm.GraphicalModelClusterer;
import edu.umass.ciir.fws.clustering.gm.Probability;
import edu.umass.ciir.fws.clustering.gm.ScoredProbItem;
import edu.umass.ciir.fws.clustering.gm.lr.LinearRegressionModel;
import edu.umass.ciir.fws.demo.search.GalagoSearchEngine;
import edu.umass.ciir.fws.feature.CandidateListDocFreqOnlineMap;
import edu.umass.ciir.fws.feature.ItemPairFeatures;
import edu.umass.ciir.fws.feature.TermFeatures;
import edu.umass.ciir.fws.feature.TermFeaturesOnlineExtractor;
import edu.umass.ciir.fws.feature.TermPairFeatureOnlineExtractor;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * 1. extract term features 2. predict term probability 3. selected top K terms
 * (according to the probability 4. extracted pair features for the selected
 * term (pair) 5. predict term pair probability 6. use term and term pair
 * probability to perform GMJ/GMI inference
 *
 * @author wkong
 */
public class GmFacetRefiner implements FacetRefiner {

    Parameters p;
    GalagoSearchEngine galago; // to get document frequency for terms
    int numTopScoredItems;
    GraphicalModelClusterer clusterer;
    CandidateListDocFreqOnlineMap clistDfMap;

    public GmFacetRefiner(Parameters p, GalagoSearchEngine galago) {
        this.p = p;
        this.galago = galago;
        clistDfMap = new CandidateListDocFreqOnlineMap(p);
        numTopScoredItems = (int) p.getLong("numTopScoredItems");
        clusterer = p.getString("facetModel").equals("gmj") ? new GmJointClusterer() : new GmIndependentClusterer(p);
    }

    @Override
    public List<ScoredFacet> refine(List<CandidateList> clists, List<RankedDocument> docs) {
        Utility.info("extracting facet term features ...");
        TermFeaturesOnlineExtractor termFeatureExtractor = new TermFeaturesOnlineExtractor(galago, clistDfMap, p);
        TreeMap<String, TermFeatures> termFeatures = termFeatureExtractor.extract(clists, docs);

        Utility.info("predicting facet term probability...");
        LinearRegressionModel termModel = new LinearRegressionModel();
        int[] termFeatureIndices = convertToIntArray(p.getAsList("termFeatureIndices", Long.class));
        File termModelFile = new File(p.getString("termModel"));
        File termScalerFile = new File(p.getString("termScaler"));
        try {
            termModel.load(termModelFile, termScalerFile, termFeatureIndices);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        Utility.info("extracting facet pair features...");
        List<ScoredProbItem> items = new ArrayList<>();
        for (String term : termFeatures.keySet()) {
            TermFeatures features = termFeatures.get(term);
            double prob = termModel.predict(features);
            items.add(new ScoredProbItem(term, prob));
        }
        Collections.sort(items);

        // select top facets
        items = items.subList(0, Math.min(items.size(), numTopScoredItems));

        TermPairFeatureOnlineExtractor pairFeatureExtractor = new TermPairFeatureOnlineExtractor();
        HashMap<String, ItemPairFeatures> pairFeatures = pairFeatureExtractor.extract(clists, docs, items);
        HashMap<String, Integer> itemIdMap = pairFeatureExtractor.itemIdMap;

        Utility.info("predicting facet term pair probability...");
        LinearRegressionModel pairModel = new LinearRegressionModel();
        int[] pairFeatureIndices = convertToIntArray(p.getAsList("pairFeatureIndices", Long.class));
        File pairModelFile = new File(p.getString("pairModel"));
        File pairScalerFile = new File(p.getString("pairScaler"));
        try {
            pairModel.load(pairModelFile, pairScalerFile, pairFeatureIndices);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        HashMap<String, Probability> pairProbs = new HashMap<>();
        for (String pid : pairFeatures.keySet()) {
            ItemPairFeatures features = pairFeatures.get(pid);
            double prob = pairModel.predict(features);
            pairProbs.put(pid, new Probability(prob));
        }
        
        Utility.info("refining facets...");
        List<ScoredFacet> facets = clusterer.cluster(items, itemIdMap, pairProbs);

        return facets;
    }

    private int[] convertToIntArray(List<Long> list) {
        int[] res = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            res[i] = list.get(i).intValue();
        }
        return res;
    }

}
