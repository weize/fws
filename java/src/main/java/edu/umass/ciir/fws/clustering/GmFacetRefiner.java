/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clustering.gm.lr.LinearRegressionModel;
import edu.umass.ciir.fws.demo.search.GalagoSearchEngine;
import edu.umass.ciir.fws.feature.TermFeatures;
import edu.umass.ciir.fws.feature.TermFeaturesOnlineExtractor;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class GmFacetRefiner implements FacetRefiner {

    Parameters p;
    GalagoSearchEngine galago;

    public GmFacetRefiner(Parameters p, GalagoSearchEngine galago) {
        this.p = p;
        this.galago = galago;
    }

    @Override
    public List<ScoredFacet> refine(List<CandidateList> clists, List<RankedDocument> docs) {
        TermFeaturesOnlineExtractor termFeatureExtractor = new TermFeaturesOnlineExtractor(galago, p);
        TreeMap<String, TermFeatures> termFeatures = termFeatureExtractor.extract(clists, docs);

        LinearRegressionModel termModel = new LinearRegressionModel();
        int[] termFeatureIndices = convertToIntArray(p.getAsList("termFeatureIndices", Long.class));
        File termModelFile = new File(p.getString("termModel"));
        File termScalerFile = new File(p.getString("termScaler"));
        try {
            termModel.load(termModelFile, termScalerFile, termFeatureIndices);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
        ArrayList<ScoredItem> scoredItems = new ArrayList<>();
        for(String term : termFeatures.keySet()) {
            TermFeatures features = termFeatures.get(term);
            double prob = termModel.predict(features);
            scoredItems.add(new ScoredItem(term, prob));
        }
        Collections.sort(scoredItems);
        for (ScoredItem item : scoredItems) {
            System.err.println(item);
        }

        return null;
    }

    private int[] convertToIntArray(List<Long> list) {
        int[] res = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            res[i] = list.get(i).intValue();
        }
        return res;
    }

}
