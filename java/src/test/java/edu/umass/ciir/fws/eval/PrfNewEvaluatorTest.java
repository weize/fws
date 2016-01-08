/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import static edu.umass.ciir.fws.eval.PrfAlphaBetaEvaluator.weightings;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author wkong
 */
public class PrfNewEvaluatorTest {

    public PrfNewEvaluatorTest() {
    }

    @Test
    public void testHarmonicMean() {
        System.out.println("testHarmonicMean");

        double p = 0.2802;
        double r = 0.4975;
        double f = 0.4018;

        for (double[] alphaBeta : PrfAlphaBetaEvaluator.alphaBetas) {
            double prf = PrfNewEvaluator.harmonicMean(p, r, f, alphaBeta[0], alphaBeta[1]);
            System.out.println(prf);
        }
    }

    @Test
    public void testEval() {
        System.out.println("eval");

        List<AnnotatedFacet> afacets = createAfacetsCase();
        List<ScoredFacet> sfacets = createSfacetCase();
        int numTopFacets = 10;
        PrfNewEvaluator instance = new PrfNewEvaluator();
        double[] expResult = {
            // term equal
            0.625, 0.5, 0.625 / (0.625 + 0.5),
            0.5, 1.0 / 13, 1.0 / 13 / (1.0 / 13 + 0.5),
            0.5, 1.0 / 3, (2.0 / 6) / (0.5 + 1.0 / 3),
            0.2702702703, 0.4918032787,
            // term rating
            0.5625, 0.45, 0.5,
            0.5, 0.07692307692, 0.1333333333,
            0.5, 0.3333333333, 0.4,
            0.2608695652, 0.4615384615,
            // facet equal
            0.5357142857, 0.4285714286, 0.4761904762,
            0.25, 0.03846153846, 0.06666666667,
            0.5, 0.3333333333, 0.4,
            0.15625, 0.447761194,
            // facet rating
            0.4910714286, 0.3928571429, 0.4365079365,
            0.25, 0.03846153846, 0.06666666667,
            0.5, 0.3333333333, 0.4,
            0.1532033426, 0.4236200257
        };
        double[] result = instance.eval(afacets, sfacets, numTopFacets);
        for (double res : result) {
            System.out.println(res);
        }
        assertArrayEquals(expResult, result, 0.1);
    }

    public List<AnnotatedFacet> createAfacetsCase() {
        // 2: ["a", "b", "c", "d"]
        // 2: ["e", "f"]
        // 1: ["g", "h", "i", "j"]
        List<AnnotatedFacet> afacets = new ArrayList<>();
        AnnotatedFacet an1 = new AnnotatedFacet(2, "1", "");
        an1.addTerm("a");
        an1.addTerm("b");
        an1.addTerm("c");
        an1.addTerm("d");
        afacets.add(an1);
        AnnotatedFacet an2 = new AnnotatedFacet(2, "1", "");
        an2.addTerm("e");
        an2.addTerm("f");
        afacets.add(an2);
        AnnotatedFacet an3 = new AnnotatedFacet(1, "1", "");
        an3.addTerm("g");
        an3.addTerm("h");
        an3.addTerm("i");
        an3.addTerm("j");
        afacets.add(an3);
        return afacets;
    }

    private List<ScoredFacet> createSfacetCase() {
        // 0.9: ["a", "b"]
        // 0.6: ["e", "k"]
        // 0.3: ["g", "c"]
        // 0.1: ["l", "m"]
        List<ScoredFacet> sfacets = new ArrayList<>();
        ScoredFacet sf1 = ScoredFacet.parseFacet("0.9\ta|b");
        ScoredFacet sf2 = ScoredFacet.parseFacet("0.6\te|k");
        ScoredFacet sf3 = ScoredFacet.parseFacet("0.3\tg|c");
        ScoredFacet sf4 = ScoredFacet.parseFacet("0.1\tl|m");
        sfacets.add(sf1);
        sfacets.add(sf2);
        sfacets.add(sf3);
        sfacets.add(sf4);
        return sfacets;
    }

}
