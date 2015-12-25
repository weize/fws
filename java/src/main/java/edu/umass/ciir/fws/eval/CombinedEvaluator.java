/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Combine other query facet evaluators together
 * @author wkong
 */
public class CombinedEvaluator {

    PrfEvaluator prfEvaluator;
    RpndcgEvaluator rpndcgEvaluator;
    ClusteringEvaluator clusteringEvaluator;
    final static int metricNum = PrfEvaluator.metricNum + RpndcgEvaluator.metricNum + ClusteringEvaluator.metricNum;

    HashMap<String, FacetAnnotation> facetMap;

    public CombinedEvaluator(int numTopFacets, File annotatedFacetTextFile) throws IOException {
        prfEvaluator = new PrfEvaluator(numTopFacets);
        rpndcgEvaluator = new RpndcgEvaluator(numTopFacets);
        clusteringEvaluator = new ClusteringEvaluator(numTopFacets);
        facetMap = FacetAnnotation.loadAsMapFromTextFile(annotatedFacetTextFile);
    }
    
    public CombinedEvaluator(File annotatedFacetTextFile) throws IOException {
        prfEvaluator = new PrfEvaluator(10);
        rpndcgEvaluator = new RpndcgEvaluator(10);
        clusteringEvaluator = new ClusteringEvaluator(10);
        facetMap = FacetAnnotation.loadAsMapFromTextFile(annotatedFacetTextFile);
    }

    public void eval(File queryFile, String facetDir, String model, String paramFileNameStr, File outfile, int numTopFacets) throws IOException {
        List<TfQuery> queries = QueryFileParser.loadQueries(queryFile);
        double[] avg = new double[metricNum];
        ArrayList<QueryMetrics> results = new ArrayList<>();
        for (TfQuery query : queries) {
            FacetAnnotation annotator = facetMap.get(query.id);
            File systemFile = new File(Utility.getFacetFileName(facetDir, query.id, model, paramFileNameStr));
            List<ScoredFacet> system = ScoredFacet.loadFacets(systemFile);
            double[] result = eval(annotator.facets, system, numTopFacets);
            results.add(new QueryMetrics(query.id, result));
            Utility.add(avg, result);
        }
        Utility.avg(avg, queries.size());
        results.add(new QueryMetrics("all", avg));
        QueryMetrics.output(results, outfile);
    }

    public double[] eval(ArrayList<AnnotatedFacet> facets, List<ScoredFacet> system, int numTopFacets) throws IOException {
        double[] scores = new double[metricNum];
        int i = 0;
        for (double score : prfEvaluator.eval(facets, system, numTopFacets)) {
            scores[i] = score;
            i++;
        }

        for (double score : rpndcgEvaluator.eval(facets, system, numTopFacets)) {
            scores[i] = score;
            i++;
        }

        for (double score : clusteringEvaluator.eval(facets, system, numTopFacets)) {
            scores[i] = score;
            i++;
        }
        return scores;
    }

    public static double f1(double p, double r) {
        return p + r < Utility.epsilon ? 0 : 2 * p * r / (p + r);
    }

}
