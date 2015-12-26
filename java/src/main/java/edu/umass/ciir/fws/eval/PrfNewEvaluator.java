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

    private static final int metricNum = 22;
    List<ScoredFacet> sysFacets; // system
    List<AnnotatedFacet> annFacets; // annotators

    double alpha = 1;
    double beta = 1;

    Map<String, Double> itemWeightMap; // only stored postive items
    Set<String> systemItemSet; // all items in the system facets
    List<HashSet<String>> itemSets; // stores annotator sysFacets as sets

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
        
        double [] noWeight = eval(false);
        double [] hasWeight = eval(true);
        double [] all = new double[metricNum];
        int i = 0;
        for(double res : noWeight) {
            all[i++] = res;
        }
        for(double res : hasWeight) {
            all[i++] = res;
        }
        return all;
    }
    
    public double[] eval(boolean toWeight) {
         // term precisoin recall
        double[] termPRF = termPrecisionRecallF1(toWeight);
        // pair precision recall (for clustering)
        double[] pairPRFComplete = pairPrecisionRecallF1(toWeight, false);
        double[] pairPRFOverlap = pairPrecisionRecallF1(toWeight, true);

        double prfComplete = harmonicMean(termPRF[0], termPRF[1], pairPRFComplete[2]);
        double prfOverlap = harmonicMean(termPRF[0], termPRF[1], pairPRFOverlap[2]);
        
        return new double[] {termPRF[0], termPRF[1], termPRF[2],
          pairPRFComplete[0], pairPRFComplete[1], pairPRFComplete[2],
          pairPRFOverlap[0], pairPRFOverlap[1], pairPRFOverlap[2],
          prfComplete, prfOverlap
        };    
    }

    private void loadItemWeightMap() {
        itemWeightMap.clear();
        for (AnnotatedFacet facet : annFacets) {
            for (String item : facet.terms) {
                double weight = facet.rating;
                //double weight = ((double)facet.rating) / facet.terms.size();
                itemWeightMap.put(item, weight);
            }
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

    private double[] termPrecisionRecallF1(boolean toWeight) {
        double correct = 0;
        double stotal = 0;
        double atotal = 0;

        HashSet<String> set = new HashSet<>();
        for (ScoredFacet facet : sysFacets) {
            for (ScoredItem item : facet.items) {
                String term = item.item;
                if (!set.contains(term)) {
                    double weight = weight(term, toWeight);
                    if (itemWeightMap.containsKey(term)) {
                        correct += weight;
                    }
                    stotal += weight;
                    set.add(term);
                }
            }
        }

        for (String item : itemWeightMap.keySet()) {
            double weight = weight(item, toWeight);
            atotal += weight;
        }

        double precision = stotal < Utility.epsilon ? 0 : correct / stotal;
        double recall = atotal < Utility.epsilon ? 0 : correct / atotal;
        double f1 = CombinedFacetEvaluator.f1(precision, recall);
        return new double[]{precision, recall, f1};
    }

    /**
     *
     * @param toWeight
     * @param overalap only consider system items that also appear in annotator
     * facets, otherwise also consider annotator terms that are not in system
     * facets (assume they are singletons).
     * @return
     */
    private double[] pairPrecisionRecallF1(boolean toWeight, boolean overalap) {

        double correct = 0;
        double aTotal = 0; // annotator
        double sTotal = 0; // system

        for (ScoredFacet facet : sysFacets) {
            for (int i = 0; i < facet.items.size(); i++) {
                String item1 = facet.items.get(i).item;
                if (itemWeightMap.containsKey(item1)) {
                    for (int j = i + 1; j < facet.items.size(); j++) {
                        String item2 = facet.items.get(j).item;
                        if (itemWeightMap.containsKey(item2)) {
                            double weight = weight(item1, item2, toWeight);
                            sTotal += weight;
                            // correct ?
                            if (inSameItemSet(item1, item2)) {
                                correct += weight;
                            }
                        }

                    }
                }
            }
        }

        // only consider terms in both system and annoator facets
        for (AnnotatedFacet afacet : annFacets) {
            int size = 0;
            for (String term : afacet.terms) {
                if (!overalap || systemItemSet.contains(term)) {
                    size++;
                }
            }
            double weight = weight(afacet.get(0), afacet.get(0), toWeight);
            aTotal += size * (size - 1) * weight / 2;
        }

        double precision = sTotal == 0 ? 0 : correct / sTotal;
        double recall = aTotal == 0 ? 0 : correct / aTotal;
        double f1 = CombinedFacetEvaluator.f1(precision, recall);
        return new double[]{precision, recall, f1};
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

    private double weight(String item, boolean toWeight) {
        return toWeight && itemWeightMap.containsKey(item) ? itemWeightMap.get(item) : 1;
    }

    private double weight(String kp, String kp2, boolean toWeight) {
        return toWeight ? weight(kp, true) + weight(kp2, true) : 2;
    }

    @Override
    public int metricNum() {
        return metricNum;
    }

}
