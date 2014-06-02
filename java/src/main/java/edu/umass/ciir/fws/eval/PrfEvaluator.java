/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.lemurproject.galago.tupleflow.Utility;

/**
 *
 * @author wkong
 */
public class PrfEvaluator {

    public static final int metricNum = 10;
    int numTopFacets;
    List<ScoredFacet> sysFacets; // system
    List<AnnotatedFacet> annFacets; // annotators

    double alpha = 1;
    double beta = 1;

    Map<String, Double> itemWeightMap; // only stored postive items
    List<HashSet<String>> itemSets; // stores annotator sysFacets as sets

    public PrfEvaluator(int numTopFacets) {
        this.numTopFacets = numTopFacets;
        itemWeightMap = new HashMap<>();
        itemSets = new ArrayList<>();
    }

    protected void loadFacets(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets) {
        // only using top n sysFacets
        sysFacets = sfacets.subList(0, Math.min(sfacets.size(), numTopFacets));
        annFacets = new ArrayList<>();
        for (AnnotatedFacet f : afacets) {
            if (f.isValid()) {
                annFacets.add(f);
            }
        }
    }

    public double[] eval(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets, int numTopFacets) {
        this.numTopFacets = numTopFacets;
        loadFacets(afacets, sfacets);
        loadItemWeightMap();
        loadItemSets();

        double p = precision(false);
        double wp = precision(true);
        double r = recall(false);
        double wr = recall(true);

        double f1 = QueryFacetEvaluator.f1(p, r);
        double wf1 = QueryFacetEvaluator.f1(wp, wr);

        double f1c = clusteringF1(false);
        double wf1c = clusteringF1(true);

        double prf = hamitionMean(p, r, f1c);
        double wprf = hamitionMean(wp, wr, wf1c);

        return new double[]{p, wp, r, wr, f1, wf1, f1c, wf1c, prf, wprf};
    }

    /**
     *
     * @param afacets sysFacets from annotator
     * @param sfacets sysFacets from system
     * @return
     */
    public double[] eval(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets) {
        return eval(afacets, sfacets, this.numTopFacets);
    }

    private void loadItemWeightMap() {
        itemWeightMap.clear();
        for (AnnotatedFacet facet : annFacets) {
            for (String item : facet.terms) {
                double weight = facet.rating;
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

    private double precision(boolean toWeight) {
        double correct = 0;
        double total = 0;

        HashSet<String> set = new HashSet<>();
        for (ScoredFacet facet : sysFacets) {
            for (ScoredItem item : facet.items) {
                String term = item.item;
                if (!set.contains(term)) {
                    double weight = weight(term, toWeight);
                    if (itemWeightMap.containsKey(term)) {
                        correct += weight;
                    }
                    total += weight;
                    set.add(term);
                }
            }
        }

        return total < Utility.epsilon ? 0 : correct / total;
    }

    private double recall(boolean toWeight) {
        double correct = 0;
        double total = 0;

        for (String item : itemWeightMap.keySet()) {
            double weight = weight(item, toWeight);
            total += weight;
        }

        HashSet<String> set = new HashSet<>();

        for (ScoredFacet facet : sysFacets) {
            for (ScoredItem item : facet.items) {
                String term = item.item;
                if (!set.contains(term)) {
                    double weight = weight(term, toWeight);
                    if (itemWeightMap.containsKey(term)) {
                        correct += weight;
                    }
                    set.add(term);
                }
            }
        }

        return correct / total;
    }

    private double clusteringF1(boolean toWeight) {

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

        for (AnnotatedFacet afacet : annFacets) {
            int size = afacet.size();
            double weight = weight(afacet.get(0), afacet.get(0), toWeight);
            aTotal += size * (size - 1) * weight / 2;
        }

        double p = sTotal == 0 ? 0 : correct / sTotal;
        double r = aTotal == 0 ? 0 : correct / aTotal;
        return QueryFacetEvaluator.f1(p, r);
    }

    private double hamitionMean(double p, double r, double f) {
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

}
