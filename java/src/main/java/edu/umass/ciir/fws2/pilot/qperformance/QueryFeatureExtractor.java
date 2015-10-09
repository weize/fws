/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws2.pilot.qperformance;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import static edu.umass.ciir.fws.clustering.gm.GraphicalModelClusterer.numTopScoredItems;
import edu.umass.ciir.fws.clustering.gm.ScoredProbItem;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQuery", order = {"+id"})
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters", order = {"+id"})
public class QueryFeatureExtractor extends StandardStep<TfQuery, TfQueryParameters> {

    String facetDir;
    String facetModel;
    String facetParam;
    String gmPredictDir;

    int topK = 10;
    List<ScoredFacet> facets;
    QueryFeatures features;
    TfQuery q;
    HashMap<String, Integer> itemIdMap;

    public QueryFeatureExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        facetModel = p.getString("facetModel");
        facetDir = Utility.getFileName(p.getString("facetDir"), facetModel, "facet");
        facetParam = p.getString("facetParam");
        gmPredictDir = Utility.getFileName(p.getString("gmDir"), "predict");
    }

    @Override
    public void process(TfQuery query) throws IOException {
        q = query;
        features = new QueryFeatures(q.id, q.text);

        File facetFile = new File(Utility.getFacetFileName(facetDir, q.id, facetModel, facetParam));
        facets = ScoredFacet.loadFacets(facetFile);

        // load terms
        itemIdMap = new HashMap<>();

        for (int i = 0; i < topK && i < facets.size(); i++) {
            ScoredFacet facet = facets.get(i);
            for (ScoredItem t : facet.items) {
                itemIdMap.put(t.item, itemIdMap.size());
            }
        }

        extractTermProbFeature();
        extractTermPairProbFeature();

        processor.process(features.toTfQueryParameters());
    }

    private void extractTermProbFeature() throws IOException {
        // load term prob
        double tProbMin = Double.POSITIVE_INFINITY;
        double tProbMax = Double.NEGATIVE_INFINITY;

        String tPredictFilename = Utility.getFileName(gmPredictDir, q.id, String.format("%s.t.predict", q.id));
        BufferedReader reader = Utility.getReader(tPredictFilename);
        String line;

        while ((line = reader.readLine()) != null) {
            // 0.00720595275504        -1      BIR_101118      cuttings over its lifetime
            String[] elems = line.split("\t");
            double score = Double.parseDouble(elems[0]);
            String item = elems[3];
            if (itemIdMap.containsKey(item)) {
                tProbMin = Math.min(score, tProbMin);
                tProbMax = Math.max(score, tProbMax);
            }
        }
        reader.close();

        // term prob
        double tProbSum = 0;
        double tSize = 0;

        for (int i = 0; i < 10 && i < facets.size(); i++) {
            ScoredFacet facet = facets.get(i);
            tSize += facet.items.size();
            tProbSum += facet.score;

        }

        double tProbAvg = tProbSum / (double) tSize;

        features.setFeature(tSize, QueryFeatures._tSize);
        features.setFeature(tProbSum, QueryFeatures._tProbSum);
        features.setFeature(tProbAvg, QueryFeatures._tProbAvg);
        features.setFeature(tProbMin, QueryFeatures._tProbMin);
        features.setFeature(tProbMax, QueryFeatures._tProbMax);
    }

    private void extractTermPairProbFeature() throws IOException {
        // pair prob
        int pIntraSize = 0;
        double pIntraProbSum = 0;
        double pIntraProbAvg;
        double pIntraProbMin = Double.POSITIVE_INFINITY;
        double pIntraProbMax = Double.NEGATIVE_INFINITY;

        int pInterSize = 0;
        double pInterProbSum = 0;
        double pInterProbAvg;
        double pInterProbMin = Double.POSITIVE_INFINITY;
        double pInterProbMax = Double.NEGATIVE_INFINITY;

        // load pair prob
        HashMap<String, Double> pProbMap = new HashMap<>();

        String pPredictFilename = Utility.getFileName(gmPredictDir, q.id, String.format("%s.p.predict.gz", q.id));
        BufferedReader reader = Utility.getReader(pPredictFilename);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            double prob = Double.parseDouble(elems[0]);
            String[] fields = elems[3].split("\\|");
            String item1 = fields[0];
            String item2 = fields[1];
            if (itemIdMap.containsKey(item1) && itemIdMap.containsKey(item2)) {
                pProbMap.put(getItemPairId(item1, item2), prob);
            }

        }
        reader.close();

        // aggreate
        for (int i = 0; i < 10 && i < facets.size(); i++) {
            List<ScoredItem> items = facets.get(i).items;
            for (int j = 0; j < items.size(); j++) {
                for (int k = j + 1; k < items.size(); k++) {
                    double pProb = pProbMap.get(getItemPairId(items.get(j).item, items.get(k).item));
                    pIntraSize++;
                    pIntraProbSum += pProb;
                    pIntraProbMin = Math.min(pProb, pIntraProbMin);
                    pIntraProbMax = Math.max(pProb, pIntraProbMax);

                }
                // other facets
                for (int ii = i + 1; ii < 10 && ii < facets.size(); ii++) {
                    for (ScoredItem t : facets.get(ii).items) {
                        double pProb = pProbMap.get(getItemPairId(items.get(j).item, t.item));
                        pInterSize++;
                        pInterProbSum += pProb;
                        pInterProbMin = Math.min(pProb, pInterProbMin);
                        pInterProbMax = Math.max(pProb, pInterProbMax);
                    }
                }
            }
        }

        pInterProbAvg = pInterProbSum / (double) pInterSize;
        pIntraProbAvg = pIntraProbSum / (double) pIntraSize;

        features.setFeature(pInterSize, QueryFeatures._pInterSize);
        features.setFeature(pIntraSize, QueryFeatures._pIntraSize);
        features.setFeature(pInterProbSum, QueryFeatures._pInterProbSum);
        features.setFeature(pIntraProbSum, QueryFeatures._pIntraProbSum);
        features.setFeature(pInterProbAvg, QueryFeatures._pInterProbAvg);
        features.setFeature(pIntraProbAvg, QueryFeatures._pIntraProbAvg);
        features.setFeature(pInterProbMin, QueryFeatures._pInterProbMin);
        features.setFeature(pIntraProbMin, QueryFeatures._pIntraProbMin);
        features.setFeature(pInterProbMax, QueryFeatures._pInterProbMax);
        features.setFeature(pIntraProbMax, QueryFeatures._pIntraProbMax);

    }

    public String getItemPairId(String item1, String item2) {
        return getItemPairId(itemIdMap.get(item1), itemIdMap.get(item2));
    }

    public String getItemPairId(int a, int b) {
        return a < b ? a + "_" + b : b + "_" + a;
    }

}
