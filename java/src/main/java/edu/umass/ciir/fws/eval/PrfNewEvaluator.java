/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lemurproject.galago.tupleflow.Utility;

/**
 *
 * @author wkong
 */
public class PrfNewEvaluator implements QueryFacetEvaluator {

    private static final int metricNum = 44; // 4 weighting X 11 metrics
    List<ScoredFacet> sysFacets; // system
    List<AnnotatedFacet> annFacets; // annotators

    double alpha = 1;
    double beta = 1;

    Map<String, double[]> itemWeightMap; // only stored postive items
    Set<String> systemItemSet; // all items in the system facets
    List<HashSet<String>> itemSets; // stores annotator sysFacets as sets
    double[][] idealTermCW; // idea clumulative term weight [#num of postive cases][#different weigting methods]
    double[][] idealPairCWComplete; // idea clumulative Pair weight for complete case
    double[][] idealPairCWOverlap; // idea clumulative Pair weight for overlap case

    public static enum TermWeighting {

        TermEqual, TermRating, FacetEqual, FacetRating;

        public static int size() {
            return TermWeighting.values().length;
        }
    }

    public PrfNewEvaluator() {
        itemWeightMap = new HashMap<>();
        itemSets = new ArrayList<>();
        systemItemSet = new HashSet<>();
    }

    protected void loadFacets(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets, int numTopFacets) {
        // only using top n sysFacets
        sysFacets = sfacets.subList(0, Math.min(sfacets.size(), numTopFacets));
        annFacets = new ArrayList<>();
        for (AnnotatedFacet f : afacets) {
            if (f.isValid()) {
                annFacets.add(f);
            }
        }
    }

    @Override
    public double[] eval(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets, int numTopFacets) {
        loadFacets(afacets, sfacets, numTopFacets);
        loadItemWeightMap();
        loadItemSets();
        loadSystemItemSet();
        cumulateTermWeights();
        cumulatePairWeights();

        double[] all = new double[metricNum];
        int i = 0;
        for (TermWeighting weighting : TermWeighting.values()) {
            double[] results = eval(weighting);
            for (double res : results) {
                all[i++] = res;
            }
        }

        return all;
    }

    public double[] eval(TermWeighting weighting) {
        // term precisoin recall
        double[] termPRF = termPrecisionRecallF1(weighting);
        // pair precision recall (for clustering)
        double[] pairPRFComplete = pairPrecisionRecallF1(weighting, false);
        double[] pairPRFOverlap = pairPrecisionRecallF1(weighting, true);

        double prfComplete = harmonicMean(termPRF[0], termPRF[1], pairPRFComplete[2]);
        double prfOverlap = harmonicMean(termPRF[0], termPRF[1], pairPRFOverlap[2]);

        return new double[]{termPRF[0], termPRF[1], termPRF[2],
            pairPRFComplete[0], pairPRFComplete[1], pairPRFComplete[2],
            pairPRFOverlap[0], pairPRFOverlap[1], pairPRFOverlap[2],
            prfComplete, prfOverlap
        };
    }

    private void loadItemWeightMap() {
        itemWeightMap.clear();
        for (AnnotatedFacet facet : annFacets) {
            for (String item : facet.terms) {
                double[] weights = new double[TermWeighting.size()];
                weights[TermWeighting.TermEqual.ordinal()] = 1;
                weights[TermWeighting.TermRating.ordinal()] = facet.rating;
                weights[TermWeighting.FacetEqual.ordinal()] = 1.0 / (double) facet.size();
                weights[TermWeighting.FacetRating.ordinal()] = (double) facet.rating / (double) facet.size();
                itemWeightMap.put(item, weights);
            }
        }

    }

    private void cumulateTermWeights() {
        // compute idea cumulative weighting
        double[][] termWeights = itemWeightMap.values().toArray(new double[0][]);
        idealTermCW = cumulateWeights(termWeights);
    }

    private void cumulatePairWeights() {
        // compute idea cumulative weighting
        ArrayList<double[]> pairWeightListComplete = new ArrayList<>();
        ArrayList<double[]> pairWeightListOverlap = new ArrayList<>();

        for (AnnotatedFacet facet : annFacets) {
            List<String> terms = facet.terms;
            for (int i = 0; i < terms.size(); i++) {
                String t1 = terms.get(i);
                for (int j = i + 1; j < terms.size(); j++) {
                    String t2 = terms.get(j);
                    double[] weights = new double[TermWeighting.size()];
                    for (TermWeighting weighting : TermWeighting.values()) {
                        weights[weighting.ordinal()] = weight(t1, t2, weighting);
                    }
                    pairWeightListComplete.add(weights);
                    if (systemItemSet.contains(t1) && systemItemSet.contains(t2)) {
                        pairWeightListOverlap.add(weights);
                    }
                }
            }
        }

        double[][] pairWeightsComoplete = pairWeightListComplete.toArray(new double[0][]);
        idealPairCWComplete = cumulateWeights(pairWeightsComoplete);
        double[][] pairWeightsOverlap = pairWeightListOverlap.toArray(new double[0][]);
        idealPairCWOverlap = cumulateWeights(pairWeightsOverlap);
    }

    public double[][] cumulateWeights(double[][] weights) {
        // new idealTermCW
        int size = weights.length;
        double[][] idealCWs = new double[size][]; // ideal cumulative weights

        // new arrays
        for (int i = 0; i < size; i++) {
            idealCWs[i] = new double[TermWeighting.size()];
        }

        // compute
        for (TermWeighting weighting : TermWeighting.values()) {
            // sort by one weighting method
            Arrays.sort(weights, new WeightsComparator(weighting));
            // computer cumulative weight
            double cumulativeWeight = 0;
            int index = weighting.ordinal();
            for (int i = 0; i < size; i++) {
                cumulativeWeight += weights[i][index];
                idealCWs[i][index] = cumulativeWeight;
            }
        }

        return idealCWs;
    }

    public static class WeightsComparator implements Comparator<double[]> {

        int index;

        public WeightsComparator(TermWeighting w) {
            index = w.ordinal();
        }

        @Override
        public int compare(double[] o1, double[] o2) {
            return Double.compare(o2[index], o1[index]);
        }

    }

    private void loadItemSets() {
        itemSets.clear();
        for (AnnotatedFacet facet : annFacets) {
            HashSet<String> set = new HashSet<>();
            set.addAll(facet.terms);
            itemSets.add(set);
        }
    }

    private void loadSystemItemSet() {
        systemItemSet.clear();
        for (ScoredFacet facet : sysFacets) {
            for (ScoredItem item : facet.items) {
                systemItemSet.add(item.item);
            }
        }
    }

    private double[] termPrecisionRecallF1(TermWeighting weighting) {
        int correct = 0;
        int stotal = 0;
        int atotal = itemWeightMap.size();
        double weight = 0; // cumulative postive case weight

        HashSet<String> set = new HashSet<>();
        for (ScoredFacet facet : sysFacets) {
            for (ScoredItem item : facet.items) {
                String term = item.item;
                if (!set.contains(term)) { // do not count duplidate items
                    if (itemWeightMap.containsKey(term)) {
                        correct++;
                        weight += weight(term, weighting);
                    }
                    stotal++;
                    set.add(term);
                }
            }
        }

        double idealWeight = correct == 0 ? 0.0 : idealTermCW[correct - 1][weighting.ordinal()];
        double weightRatio = safelyDivide(weight, idealWeight);

        double precision = safelyDivide(correct, stotal) * weightRatio;
        double recall = safelyDivide(correct, atotal) * weightRatio;
        double f1 = CombinedFacetEvaluator.f1(precision, recall);
        return new double[]{precision, recall, f1};
    }

    public static double safelyDivide(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / (double) denominator;
    }

    public static double safelyDivide(double numerator, double denominator) {
        return Math.abs(denominator) < Utility.epsilon ? 0.0 : numerator / denominator;
    }

    /**
     *
     * @param weighting
     * @param overalap only consider system items that also appear in annotator
     * facets, otherwise also consider annotator terms that are not in system
     * facets (assume they are singletons).
     * @return
     */
    private double[] pairPrecisionRecallF1(TermWeighting weighting, boolean overlap) {

        int correct = 0;
        int aTotal = 0; // annotator
        int sTotal = 0; // system
        double weight = 0.0;

        HashSet<String> pairSet = new HashSet<>();
        
        for (ScoredFacet facet : sysFacets) {
            for (int i = 0; i < facet.items.size(); i++) {
                String item1 = facet.items.get(i).item;
                if (itemWeightMap.containsKey(item1)) {
                    for (int j = i + 1; j < facet.items.size(); j++) {
                        String item2 = facet.items.get(j).item;
                        String pairId = getPairId(item1, item2);
                        if (pairSet.contains(pairId)) { // do not count duplidates
                            continue;
                        } else {
                            pairSet.add(pairId);
                        }
                        if (itemWeightMap.containsKey(item2)) {
                            sTotal++;
                            // correct ?
                            if (inSameItemSet(item1, item2)) {
                                correct++;
                                weight += weight(item1, item2, weighting);
                            }
                        }

                    }
                }
            }
        }

        double idealWeight = correct == 0 ? 0.0
                : (overlap ? idealPairCWOverlap[correct - 1][weighting.ordinal()]
                : idealPairCWComplete[correct - 1][weighting.ordinal()]);
        double weightRatio = safelyDivide(weight, idealWeight);

        // only consider terms in both system and annoator facets
        for (AnnotatedFacet afacet : annFacets) {
            int size = 0;
            for (String term : afacet.terms) {
                if (!overlap || systemItemSet.contains(term)) {
                    size++;
                }
            }

            aTotal += size * (size - 1) / 2;
        }

        double precision = safelyDivide(correct, sTotal) * weightRatio;
        double recall = safelyDivide(correct, aTotal) * weightRatio;
        double f1 = CombinedFacetEvaluator.f1(precision, recall);
        return new double[]{precision, recall, f1};
    }

    private String getPairId(String item1, String item2) {
        return item1.compareTo(item2) < 0 ? item1 + "|" + item2 : item2 + "|" + item1;
    }
    
    private double harmonicMean(double p, double r, double f) {
        double a = alpha * alpha;
        double b = beta * beta;
        double d = a * r * f + b * p * f + p * r;
        return d < Utility.epsilon ? 0 : p * r * f * (a + b + 1) / d;
    }

    private boolean inSameItemSet(String item1, String item2) {
        for (HashSet<String> set : itemSets) {
            if (set.contains(item1) && set.contains(item2)) {
                return true;
            }
        }
        return false;
    }

    private double weight(String item, TermWeighting weighting) {
        return itemWeightMap.get(item)[weighting.ordinal()];
    }

    private double weight(String kp, String kp2, TermWeighting weighting) {
        return weight(kp, weighting) + weight(kp2, weighting);
    }

    @Override
    public int metricNum() {
        return metricNum;
    }

}
