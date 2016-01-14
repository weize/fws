/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.qperformance;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.eval.CombinedFacetEvaluator;
import edu.umass.ciir.fws.eval.PrfAlphaBetaEvaluator;
import static edu.umass.ciir.fws.eval.PrfNewEvaluator.safelyNormalize;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author wkong
 */
public class QPfeatureExtractor {

    QueryPerformanceFeatures features;
    List<ScoredFacet> facets;
    HashMap<String, Integer> itemIdMap;
    HashMap<String, Double> scoreMap; // qid -> score

    double tP;
    double tR;
    double pP;
    double pR;

    public HashMap<String, QueryPerformanceFeatures> run(File queryFile,
            String facetTuneDir, String facetModel, String modelParams, int topK,
            String gmPredictDir,
            int metricIdx) throws IOException {
        // facet-tune-gm-prf_b05_ga-allcase-f10/gmi
        String modelDir = Utility.getFileName(facetTuneDir, facetModel);
        // facet-tune-gm-prf_b05_ga-allcase-f10/gmi/eval/gmi.sum-31.1.eval
        File evalFile = new File(Utility.getFileName(modelDir, "eval",
                String.format("%s.%s.%d.eval", facetModel, modelParams, topK)));
        //gmi/facet
        String facetDir = Utility.getFileName(modelDir, "facet");
        scoreMap = loadEvalScores(evalFile, metricIdx);

        HashMap<String, QueryPerformanceFeatures> all = new HashMap<>();
        for (TfQuery query : QueryFileParser.loadQueries(queryFile)) {
            //gmi/facet/102/102.gmi.sum-45.facet
            File facetFile = new File(Utility.getFileName(facetDir, query.id,
                    String.format("%s.%s.%s.facet", query.id, facetModel, modelParams)));

            // termPredictFile
            File termPredictFile = new File(Utility.getFileName(gmPredictDir, query.id,
                    String.format("%s.t.predict", query.id)));
            // 
            File pairPredictFile = new File(Utility.getFileName(gmPredictDir, query.id,
                    String.format("%s.p.predict.gz", query.id)));
            all.put(query.id, extract(query, facetFile, topK, termPredictFile, pairPredictFile));
        }

        return all;
    }

    private HashMap<String, Double> loadEvalScores(File evalFile, int metricIdx) throws IOException {
        HashMap<String, Double> scoreMap = new HashMap<>();
        BufferedReader reader = Utility.getReader(evalFile);
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().startsWith("#")) {
                QueryMetrics qm = QueryMetrics.parse(line);
                if (!qm.qid.equals("all")) {
                    scoreMap.put(qm.qid, qm.values[metricIdx]);
                }
            }
        }
        reader.close();
        return scoreMap;
    }

    public QueryPerformanceFeatures extract(TfQuery query,
            File facetFile, int topK, File termPredictFile, File pairPredictFile) throws IOException {

        features = new QueryPerformanceFeatures(query.id, query.text, scoreMap.get(query.id));
        facets = ScoredFacet.loadFacets(facetFile, topK);

        // load terms
        itemIdMap = new HashMap<>();

        for (int i = 0; i < topK && i < facets.size(); i++) {
            ScoredFacet facet = facets.get(i);
            for (ScoredItem t : facet.items) {
                itemIdMap.put(t.item, itemIdMap.size());
            }
        }

        extractTermProbFeature(termPredictFile);
        extractTermPairProbFeature(pairPredictFile);

        extractPRFFeatures();
        return features;
    }

    private void extractTermProbFeature(File termPredictFile) throws IOException {
        // load term prob
        double tProbMin = Double.POSITIVE_INFINITY;
        double tProbMax = Double.NEGATIVE_INFINITY;

        double llSum = 0; //log likelihod
        double allProbSum = 0;
        BufferedReader reader = Utility.getReader(termPredictFile);
        String line;

        while ((line = reader.readLine()) != null) {
            // 0.00720595275504        -1      BIR_101118      cuttings over its lifetime
            String[] elems = line.split("\t");
            double score = Double.parseDouble(elems[0]);
            String item = elems[3];
            if (itemIdMap.containsKey(item)) {
                tProbMin = Math.min(score, tProbMin);
                tProbMax = Math.max(score, tProbMax);
                llSum += safelyLog(score);
            } else {
                llSum += safelyLog(1 - score);
            }
            allProbSum += score;
        }
        reader.close();

        // term prob
        double tProbSum = 0;
        double tSize = 0;

        for (int i = 0; i < facets.size(); i++) {
            ScoredFacet facet = facets.get(i);
            tSize += facet.items.size();
            tProbSum += facet.score;

        }

        double tProbAvg;
        double llAvg = 0;
        if (tSize == 0) {
            tProbSum = 0.000001;
            tProbMax = 0.000001;
            tProbMin = 0.000001;
            tProbAvg = 0.000001;
        } else {
            tProbAvg = tProbSum / (double) tSize;
            llAvg = llSum / tSize;
        }

        tP = tProbAvg;
        tR = safelyNormalize(tProbSum, allProbSum);

        features.setFeature(tSize, QueryFeatures._tSize);
        features.setFeature(tProbSum, QueryFeatures._tProbSum);
        features.setFeature(tProbAvg, QueryFeatures._tProbAvg);
        features.setFeature(tProbMin, QueryFeatures._tProbMin);
        features.setFeature(tProbMax, QueryFeatures._tProbMax);
        features.setFeature(llSum, QueryFeatures._tLlSum);
        features.setFeature(llAvg, QueryFeatures._tLlAvg);
    }

    public double safelyLog(double prob) {
        return prob < Utility.epsilon ? - 20 : Math.log(prob);

    }

    private void extractTermPairProbFeature(File pairPredictFile) throws IOException {
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

        double llSum = 0; // loglikelihood
        double allProbSum = 0;
        // load pair prob
        HashMap<String, Double> pProbMap = new HashMap<>();

        BufferedReader reader = Utility.getReader(pairPredictFile);
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
        for (int i = 0; i < facets.size(); i++) {
            List<ScoredItem> items = facets.get(i).items;
            for (int j = 0; j < items.size(); j++) {
                for (int k = j + 1; k < items.size(); k++) {
                    String pid = getItemPairId(items.get(j).item, items.get(k).item);
                    double pProb = pProbMap.containsKey(pid) ? pProbMap.get(pid) : 0.0001;
                    llSum += safelyLog(pProb);
                    pIntraSize++;
                    pIntraProbSum += pProb;
                    pIntraProbMin = Math.min(pProb, pIntraProbMin);
                    pIntraProbMax = Math.max(pProb, pIntraProbMax);
                }
                // other facets
                for (int ii = i + 1; ii < facets.size(); ii++) {
                    for (ScoredItem t : facets.get(ii).items) {
                        String pid = getItemPairId(items.get(j).item, t.item);
                        double pProb = pProbMap.containsKey(pid) ? pProbMap.get(pid) : 0.0001;
                        llSum += safelyLog(1 - pProb);
                        pInterSize++;
                        pInterProbSum += pProb;
                        pInterProbMin = Math.min(pProb, pInterProbMin);
                        pInterProbMax = Math.max(pProb, pInterProbMax);
                    }
                }
            }
        }

        if (pInterSize == 0) {
            pInterProbAvg = 0.0000001; // no inter pairs
            pInterProbMin = 0.0000001; // no inter pairs
            pInterProbMax = 0.0000001; // no inter pairs
            pInterProbSum = 0.0000001;
            System.err.println("pInterSize = 0");
        } else {
            pInterProbAvg = pInterProbSum / (double) pInterSize;
        }

        if (pIntraSize == 0) {
            pIntraProbAvg = 0.9999999;
            pIntraProbMin = 0.9999999;
            pIntraProbMax = 0.9999999;
            pIntraProbSum = 0.9999999;
            System.err.println("pIntraSize = 0");
        } else {
            pIntraProbAvg = pIntraProbSum / (double) pIntraSize;
        }

        allProbSum = pIntraProbSum + pInterProbSum;
        pP = pIntraProbAvg;
        pR = safelyNormalize(pIntraProbSum, allProbSum);

        double llAvg = 0;
        if (pInterSize + pIntraSize != 0) {
            llAvg = llSum / (pInterSize + pIntraSize);
        }

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
        features.setFeature(llSum, QueryFeatures._pLlSum);
        features.setFeature(llAvg, QueryFeatures._pLlAvg);

    }

    public String getItemPairId(String item1, String item2) {
        return getItemPairId(itemIdMap.get(item1), itemIdMap.get(item2));
    }

    public String getItemPairId(int a, int b) {
        return a < b ? a + "_" + b : b + "_" + a;
    }

    private void extractPRFFeatures() {
        double tF = CombinedFacetEvaluator.f1(tP, tR);
        double pF = CombinedFacetEvaluator.f1(pP, pR);
        double prf = PrfAlphaBetaEvaluator.harmonicMean(tP, tR, pF, 1.0, 1.0);
        //features.setFeature(tP, QueryFeatures._tP);
        features.setFeature(tR, QueryFeatures._tR);
        features.setFeature(tF, QueryFeatures._tF);
        //features.setFeature(pP, QueryFeatures._pP);
        features.setFeature(pR, QueryFeatures._pR);
        features.setFeature(pF, QueryFeatures._pF);
        features.setFeature(prf, QueryFeatures._prf);
    }

}
