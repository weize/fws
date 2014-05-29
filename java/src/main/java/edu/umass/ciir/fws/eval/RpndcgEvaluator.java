/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.tupleflow.Utility;

/**
 *
 * @author wkong
 */
public class RpndcgEvaluator {

    public final static int metricNum = 4;
    int numTopFacets;
    List<PrFacet> sysFacets; // system
    List<PrFacet> annFacets; // annotators

    public RpndcgEvaluator(int numTopFacets) {
        this.numTopFacets = numTopFacets;
    }

    private void loadFacets(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets) {
        // only using top n sysFacets
        sysFacets = new ArrayList<>();
        for (ScoredFacet facet : sfacets.subList(0, Math.min(sfacets.size(), numTopFacets))) {
            ArrayList<String> items = new ArrayList<>();
            for (ScoredItem item : facet.items) {
                items.add(item.item);
            }
            sysFacets.add(new PrFacet(0, items));
        }

        annFacets = new ArrayList<>();
        for (AnnotatedFacet facet : afacets) {
            if (facet.isValid()) {
                annFacets.add(new PrFacet(facet.rating, facet.terms, 1, 1));
            }
        }
    }

    /**
     *
     * @param afacets facets from annotator
     * @param sfacets facet from system
     */
    public double[] eval(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets) throws IOException {
        loadFacets(afacets, sfacets);

        // map system sysFacets to annotator sysFacets
        for (PrFacet facet : sysFacets) {
            mapToAnnotatorFacet(facet);
        }

        double idealDCG = idealDCG();

        System.err.println("idealDCG: " + idealDCG);

        // different weight: no weight,  precision, precison * recall, f1
        double[] scores = new double[metricNum];
        for (int flag = 0; flag < metricNum; flag++) {
            double DCG = weightedDCG(sysFacets, flag);
            double nDCG = DCG / idealDCG;
            scores[flag] = nDCG;
        }
        return scores;
    }

    private double idealDCG() throws IOException {
        // sort rated aspects
        Collections.sort(annFacets);

        List<PrFacet> idealTopFacets = annFacets.subList(0, Math.min(annFacets.size(), numTopFacets));

        return weightedDCG(idealTopFacets, 0);
    }

    private double weightedDCG(List<PrFacet> facets, int flag) throws IOException {
        int rank = 0;
        double dcg = 0;
        for (PrFacet f : facets) {
            rank++;
            //System.err.println("rating: " + f.rating);
            dcg += (Math.pow(2, f.rating) - 1) / log2(rank + 1) * weight(f, flag);
        }
        return dcg;
    }

    public double log2(double a) {
        return Math.log(a) / Utility.log2;
    }

    private double weight(PrFacet facet, int flag) throws IOException {
        switch (flag) {
            case 0:
                return 1;
            case 1:
                return facet.precision;
            case 2:
                return facet.precision * facet.recall;
            case 3:
                return QueryFacetEvaluator.f1(facet.precision, facet.recall);
            default:
                throw new IOException("weight function flag error");
        }
    }

    private void mapToAnnotatorFacet(PrFacet sysFacet) {

        HashSet<String> sysItems = new HashSet<>();
        sysItems.addAll(sysFacet.items);

        int overlapMax = 0;
        double overlapMaxRating = -1;
        PrFacet overlapMaxAnnFacet = null;

        for (PrFacet aFacet : annFacets) {
            if (!aFacet.mapped) {
                int overlap = overlap(sysItems, aFacet.items);
                if (overlap > overlapMax
                        || (overlap == overlapMax && aFacet.rating > overlapMaxRating)) {
                    overlapMax = overlap;
                    overlapMaxRating = aFacet.rating;
                    overlapMaxAnnFacet = aFacet;
                }
            }
        }

        if (overlapMax > 0) {
            overlapMaxAnnFacet.mapped = true;
            sysFacet.rating = overlapMaxRating;
            sysFacet.precision = (double) overlapMax / (double) sysFacet.items.size();
            sysFacet.recall = (double) overlapMax / (double) overlapMaxAnnFacet.items.size();
        } else {
            sysFacet.rating = 0; // not mapped to any
            sysFacet.precision = 0;
            sysFacet.recall = 0;
        }
    }

    private int overlap(HashSet<String> sysItems, List<String> items) {
        int count = 0;
        for (String item : items) {
            if (sysItems.contains(item)) {
                count++;
            }
        }
        return count;
    }

    private static class PrFacet implements Comparable<PrFacet> {

        double precision;
        List<String> items;
        double recall;
        boolean mapped;
        double rating;

        public PrFacet(double rating, List<String> items) {
            this.rating = rating;
            this.items = items;
            this.precision = 0;
            this.recall = 0;
            this.mapped = false;
        }

        public PrFacet(double rating, List<String> items, double precision, double recall) {
            
            this.rating = rating;
            this.items = items;
            this.precision = precision;
            this.recall = recall;
            this.mapped = false;
        }

        @Override
        public int compareTo(PrFacet other) {
            return Double.compare(other.rating, this.rating);
        }

    }
}
