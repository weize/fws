/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.tupleflow.Utility;

/**
 *
 * @author wkong
 */
public class ClusteringEvaluator {

    public static final int metricNum = 2;
    int numTopFacets;
    List<HashSet<String>> annFacets;
    List<HashSet<String>> sysFacets;
    int itemNumInAnnFacets;

    public ClusteringEvaluator(int numTopFacets) {
        this.numTopFacets = numTopFacets;
    }

    public double[] eval(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets, int numTopFacets) {
        this.numTopFacets = numTopFacets;
        loadFacets(afacets, sfacets);
        double purity = purity();
        double nmi = nmi();

        return new double[]{purity, nmi};
    }
    
    /**
     *
     * @param afacets sysFacets from annotator
     * @param sfacets sysFacets from system
     * @return
     */
    public double[] eval(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets) {
        return eval(afacets, sfacets, numTopFacets);
    }

    private double nmi() {
        double mutualInfo = mutualInfo();
        double entropySys = entropy(sysFacets);
        double entropAnn = entropy(annFacets);
        double score = mutualInfo < Utility.epsilon ? 0 : mutualInfo * 2 / (entropySys + entropAnn);
        return score;
    }

    private double entropy(List<HashSet<String>> facets) {
        double score = 0;
        for (HashSet<String> facet : facets) {
            double count = facet.size();
            score -= (count / itemNumInAnnFacets) * Math.log(count / this.itemNumInAnnFacets);
        }
        return score;
    }

    private double mutualInfo() {
        double score = 0;
        for (HashSet<String> sys : sysFacets) {
            double sysSize = sys.size();
            for (HashSet<String> ann : annFacets) {
                double annSize = ann.size();
                double overlap = overlap(sys, ann);
                if (overlap != 0) {
                    score += (overlap / itemNumInAnnFacets)
                            * Math.log(itemNumInAnnFacets * overlap / (sysSize * annSize));
                }
            }
        }
        return score;
    }

    private double purity() {
        double score = 0;
        for (HashSet<String> sysFacet : sysFacets) {
            int max = maxOverlap(sysFacet);
            score += max;
        }

        score /= itemNumInAnnFacets;
        return score;
    }

    private void loadFacets(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets) {
        // annotator clusters
        annFacets = new ArrayList<>();
        HashSet<String> annItemsNeeToCover = new HashSet<>();
        itemNumInAnnFacets = 0;
        for (AnnotatedFacet facet : afacets) {
            if (facet.isValid()) {
                HashSet<String> items = new HashSet<>();
                items.addAll(facet.terms);
                annFacets.add(items);
                annItemsNeeToCover.addAll(items);
            }
        }
        itemNumInAnnFacets += annItemsNeeToCover.size();

        // system clusters
        // remove terms not in annItems
        // create singletons for terms not in system clusters
        sysFacets = new ArrayList<>();
        HashSet<String> itemsUsed = new HashSet<>();
        for (ScoredFacet facet : sfacets.subList(0, Math.min(sfacets.size(), numTopFacets))) {
            HashSet<String> items = new HashSet<>();
            for (ScoredItem item : facet.items) {
                String term = item.item;
                if (annItemsNeeToCover.contains(term)) {
                    if (!itemsUsed.contains(term)) {
                        items.add(term);
                        itemsUsed.add(term);
                        annItemsNeeToCover.remove(term);
                    }
                }
            }
            if (!items.isEmpty()) {
                sysFacets.add(items);
            }
        }
        
        for(String item : annItemsNeeToCover) {
            HashSet<String> single = new HashSet<>();
            single.add(item);
            sysFacets.add(single);
        }

    }

    private int maxOverlap(HashSet<String> sysFacet) {
        int max = 0;
        for (HashSet<String> annFacet : annFacets) {
            int count = 0;
            for (String kp : sysFacet) {
                if (annFacet.contains(kp)) {
                    count++;
                }
            }

            if (count > max) {
                max = count;
            }
        }
        return max;
    }

    private double overlap(HashSet<String> sys, HashSet<String> ann) {
        int count = 0;
        for (String item : sys) {
            if (ann.contains(item)) {
                count++;
            }
        }
        return count;
    }

}
