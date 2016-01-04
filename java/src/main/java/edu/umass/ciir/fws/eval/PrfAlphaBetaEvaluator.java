/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import java.util.List;

/**
 *
 * @author wkong
 */
public class PrfAlphaBetaEvaluator extends PrfNewEvaluator {

    private static final int metricNum = 50; // 2 weighting X (6+19) metrics
    static double[][] alphaBetas = new double[][]{
        {1.0, 1.0},
        {2.0, 1.0}, // term precision is 2X important than term recall, and clustering
        {3.0, 1.0},
        {4.0, 1.0},
        {5.0, 1.0}, // term precision is 5X important than term recall, and clustering
        {6.0, 1.0},
        {7.0, 1.0},
        {8.0, 1.0},
        {9.0, 1.0},
        {10.0, 1.0},
        {1.0, 1.0 / 2.0}, // term precision and clustering is 2X important than term recall
        {1.0, 1.0 / 3.0},
        {1.0, 1.0 / 4.0},
        {1.0, 1.0 / 5.0},
        {1.0, 1.0 / 6.0},
        {1.0, 1.0 / 7.0},
        {1.0, 1.0 / 8.0},
        {1.0, 1.0 / 9.0},
        {1.0, 1.0 / 10.0},};

    static TermWeighting[] weightings = new TermWeighting[]{TermWeighting.TermEqual, TermWeighting.TermRating};

    @Override
    public double[] eval(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets, int numTopFacets, String... params) {
        loadFacets(afacets, sfacets, numTopFacets);
        loadItemWeightMap();
        loadItemSets();
        loadSystemItemSet();
        cumulateTermWeights();
        cumulatePairWeights();

        double[] all = new double[metricNum];
        int i = 0;
        for (TermWeighting weighting : weightings) {
            double[] termPRF = termPrecisionRecallF1(weighting);
            double[] pairPRFOverlap = pairPrecisionRecallF1(weighting, true);

            i = append(all, i, termPRF);
            i = append(all, i, pairPRFOverlap);

            for (double[] alphaBeta : alphaBetas) {
                double prfOverlap = harmonicMean(termPRF[0], termPRF[1], pairPRFOverlap[2], alphaBeta[0], alphaBeta[1]);
                i = append(all, i, prfOverlap);
            }
        }

        return all;
    }

    private int append(double[] all, int start, double[] part) {
        for (double res : part) {
            all[start++] = res;
        }
        return start;
    }

    private int append(double[] all, int start, double one) {
        all[start++] = one;
        return start;
    }

    @Override
    public int metricNum() {
        return metricNum;
    }

}
