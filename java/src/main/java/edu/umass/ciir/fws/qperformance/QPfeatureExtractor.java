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
import java.util.ArrayList;
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

    List<Double> tProbs;
    List<Double> pIntraProbs;
    List<Double> pInterProbs;

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

        tProbs = new ArrayList<>();
        pIntraProbs = new ArrayList<>();
        pInterProbs = new ArrayList<>();

        extractTermProbFeature(termPredictFile);
        extractTermPairProbFeature(pairPredictFile);

        extractPRFFeatures();
        return features;
    }

    private void extractTermProbFeature(File termPredictFile) throws IOException {
        // load term prob
        tProbs.clear();

        double llSum = 0; //log likelihod 
        double tProbSumPosNeg = 0; // prob mass for all terms
        BufferedReader reader = Utility.getReader(termPredictFile);
        String line;
        while ((line = reader.readLine()) != null) {
            // 0.00720595275504        -1      BIR_101118      cuttings over its lifetime
            String[] elems = line.split("\t");
            double score = Double.parseDouble(elems[0]);
            String item = elems[3];
            if (itemIdMap.containsKey(item)) {
                tProbs.add(score);
                llSum += safelyLog(score);
            } else {
                llSum += safelyLog(1 - score);
            }            
            tProbSumPosNeg += score;
        }
        reader.close();

        double llAvg = 0;
        double[] minMaxMeanStdSum = minMaxMeanStdSum(tProbs);
        if (tProbs.isEmpty()) {
            minMaxMeanStdSum = new double[]{0, 0, 0, 0, 0};
            llAvg = 0;
        } else {
            llAvg = llSum / tProbs.size();
        }
        

        tP = minMaxMeanStdSum[2]; // E(tP) = sum_i P(t_i) / N
        tR = safelyNormalize(minMaxMeanStdSum[4], tProbSumPosNeg); // sum_i P(Tpos) / sum_i P(t_all)

        features.setFeature(tProbs.size(), QueryFeatures._tSize);
        features.setFeature(minMaxMeanStdSum[0], QueryFeatures._tProbMin);
        features.setFeature(minMaxMeanStdSum[1], QueryFeatures._tProbMax);
        features.setFeature(minMaxMeanStdSum[2], QueryFeatures._tProbAvg);
        features.setFeature(minMaxMeanStdSum[3], QueryFeatures._tProbStd);
        features.setFeature(minMaxMeanStdSum[4], QueryFeatures._tProbSum);
        features.setFeature(llSum, QueryFeatures._tLlSum);
        features.setFeature(llAvg, QueryFeatures._tLlAvg);
    }

    public double[] minMaxMeanStdSum(List<Double> nums) {
        if (nums.isEmpty()) {
            return null;
        }

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;

        for (double a : nums) {
            min = Math.min(min, a);
            max = Math.max(max, a);
            sum += a;
        }
        double mean = sum / nums.size();

        double std = 0;
        for (double a : nums) {
            double diff = mean - a;
            std += diff * diff;
        }

        std = nums.size() > 1 ? Math.sqrt(std / (nums.size() - 1)) : 0;
        return new double[]{min, max, mean, std, sum};
    }

    public double safelyLog(double prob) {
        return prob < Utility.epsilon ? - 20 : Math.log(prob);

    }

    private void extractTermPairProbFeature(File pairPredictFile) throws IOException {
        this.pIntraProbs.clear();
        this.pInterProbs.clear();

        double llSum = 0; // loglikelihood
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
                    pIntraProbs.add(pProb);
                }
                // other facets
                for (int ii = i + 1; ii < facets.size(); ii++) {
                    for (ScoredItem t : facets.get(ii).items) {
                        String pid = getItemPairId(items.get(j).item, t.item);
                        double pProb = pProbMap.containsKey(pid) ? pProbMap.get(pid) : 0.0001;
                        llSum += safelyLog(1 - pProb);
                        pInterProbs.add(pProb);
                    }
                }
            }
        }

        int pInterSize = pInterProbs.size();
        int pIntraSize = pIntraProbs.size();
        
        double [] intraMinMaxMeanStdSum = minMaxMeanStdSum(pIntraProbs);
        double [] interMinMaxMeanStdSum = minMaxMeanStdSum(pInterProbs);
        
        if (pInterSize == 0) {
            interMinMaxMeanStdSum = new double [] {0.5, 0.5, 1, 0, 0};
            System.err.println("pInterSize = 0");
        }

        if (pIntraSize == 0) {
            intraMinMaxMeanStdSum = new double [] {0, 0, 0,0, 0};
        }

        double allProbSum = intraMinMaxMeanStdSum[4] + interMinMaxMeanStdSum[4];
        pP = intraMinMaxMeanStdSum[2];
        pR = safelyNormalize(intraMinMaxMeanStdSum[4], allProbSum);

        double llAvg = 0;
        if (pInterSize + pIntraSize != 0) {
            llAvg = llSum / (pInterSize + pIntraSize);
        }

        features.setFeature(pInterSize, QueryFeatures._pInterSize);
        features.setFeature(pIntraSize, QueryFeatures._pIntraSize);
        features.setFeature(interMinMaxMeanStdSum[0], QueryFeatures._pInterProbMin);
        features.setFeature(interMinMaxMeanStdSum[1], QueryFeatures._pInterProbMax);
        features.setFeature(interMinMaxMeanStdSum[2], QueryFeatures._pInterProbAvg);
        features.setFeature(interMinMaxMeanStdSum[3], QueryFeatures._pInterProbStd);
        features.setFeature(interMinMaxMeanStdSum[4], QueryFeatures._pInterProbSum);
        features.setFeature(intraMinMaxMeanStdSum[0], QueryFeatures._pIntraProbMin);
        features.setFeature(intraMinMaxMeanStdSum[1], QueryFeatures._pIntraProbMax);
        features.setFeature(intraMinMaxMeanStdSum[2], QueryFeatures._pIntraProbAvg);
        features.setFeature(intraMinMaxMeanStdSum[3], QueryFeatures._pIntraProbStd);
        features.setFeature(intraMinMaxMeanStdSum[4], QueryFeatures._pIntraProbSum);
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
        double prfa2 = PrfAlphaBetaEvaluator.harmonicMean(tP, tR, pF, 2.0, 1.0);
        double prfb05 = PrfAlphaBetaEvaluator.harmonicMean(tP, tR, pF, 1.0, 0.5);
        //features.setFeature(tP, QueryFeatures._tP);
        features.setFeature(tR, QueryFeatures._tR);
        features.setFeature(tF, QueryFeatures._tF);
        //features.setFeature(pP, QueryFeatures._pP);
        features.setFeature(pR, QueryFeatures._pR);
        features.setFeature(pF, QueryFeatures._pF);
        features.setFeature(prf, QueryFeatures._prf);
        features.setFeature(prfa2, QueryFeatures._prfa2);
        features.setFeature(prfb05, QueryFeatures._prfb05);
        double llSum = (double)features.getFeature(QueryFeatures._tLlSum) + (double) features.getFeature(QueryFeatures._pLlSum);
        features.setFeature(llSum, QueryFeatures._llSum);
    }

}
