/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author wkong
 */
public class FacetSizeEvaluator extends PrfNewEvaluator implements QueryFacetEvaluator {

    private final static int metricNum = 7;

    @Override
    public int metricNum() {
        return metricNum;
    }

    protected double[] countTermTotal() {
        int stotal = 0;
        int stotalDup = 0;
        int atotal = itemWeightMap.size();

        HashSet<String> set = new HashSet<>();
        for (ScoredFacet facet : sysFacets) {
            for (ScoredItem item : facet.items) {
                stotalDup++;
                String term = item.item;
                if (!set.contains(term)) { // do not count duplidate items
                    stotal++;
                    set.add(term);
                }
            }
        }

        return new double[]{atotal, stotal, stotalDup};
    }

    protected double[] countPairTotal(boolean overlap) {
        int aTotal = 0; // annotator
        int sTotal = 0; // system
        int sTotalDup = 0; // system duplicated

        HashSet<String> pairSet = new HashSet<>();

        for (ScoredFacet facet : sysFacets) {
            for (int i = 0; i < facet.items.size(); i++) {
                String item1 = facet.items.get(i).item;
                if (itemWeightMap.containsKey(item1)) {
                    for (int j = i + 1; j < facet.items.size(); j++) {
                        String item2 = facet.items.get(j).item;
                        if (itemWeightMap.containsKey(item2)) {
                            sTotalDup++;
                            String pairId = getPairId(item1, item2);
                            if (!pairSet.contains(pairId)) {
                                sTotal++;
                                pairSet.add(pairId);
                            }

                        }

                    }
                }
            }
        }

        for (AnnotatedFacet afacet : annFacets) {
            int size = 0;
            for (String term : afacet.terms) {
                if (!overlap || systemItemSet.contains(term)) {
                    size++;
                }
            }

            aTotal += size * (size - 1) / 2;
        }

        return new double[]{aTotal, sTotal, sTotalDup};
    }

    @Override
    public double[] eval(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets, int numTopFacets, String... params) {
        loadFacets(afacets, sfacets, numTopFacets);
        loadItemWeightMap();
        loadItemSets();
        loadSystemItemSet();

        double[] termTotal = countTermTotal();
        double[] pairTotalComplete = countPairTotal(false);
        double[] pairTotalOverlap = countPairTotal(true);

        // annotator term size,
        // system term size distinct
        // system term size duplidate
        // annotator pair size, complete
        // annotator pair size, overlap
        // system pair size distinct
        // system pair size duplidate
        return new double[]{termTotal[0], termTotal[1], termTotal[2],
            pairTotalComplete[0], pairTotalOverlap[0], pairTotalOverlap[1], pairTotalOverlap[2]};
    }

}
