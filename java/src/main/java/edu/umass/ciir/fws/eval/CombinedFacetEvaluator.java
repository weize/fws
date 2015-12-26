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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Combine other query facet evaluators together
 * @author wkong
 */
public class CombinedFacetEvaluator implements QueryFacetEvaluator{

    List<QueryFacetEvaluator> evaluators;
    PrfEvaluator prfEvaluator;
    RpndcgEvaluator rpndcgEvaluator;
    ClusteringEvaluator clusteringEvaluator;

    HashMap<String, FacetAnnotation> facetMap;

    public CombinedFacetEvaluator(Parameters p) throws IOException {
        String annotatedFacetTextFile = p.getString("facetAnnotationText");
        facetMap = FacetAnnotation.loadAsMapFromTextFile(new File(annotatedFacetTextFile));
        List<String> evaluatorNames = p.getAsList("facetEvaluators");
        evaluators = new ArrayList<>(evaluatorNames.size());
        
        for(String className : evaluatorNames) {
            try {
                Class<?> c = Class.forName(className);
                evaluators.add((QueryFacetEvaluator)c.newInstance());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
                Logger.getLogger(CombinedFacetEvaluator.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        }
    }
    
    
//    public CombinedFacetEvaluator(File annotatedFacetTextFile) throws IOException {
//        evaluators = new ArrayList<>();
//        //evaluators.add(new PrfEvaluator(numTopFacets));
//        evaluators.add(new PrfNewEvaluator());
//        evaluators.add(new RpndcgEvaluator());
//        evaluators.add(new ClusteringEvaluator());
//        
//        
//        facetMap = FacetAnnotation.loadAsMapFromTextFile(annotatedFacetTextFile);
//    }
   
    public void eval(File queryFile, String facetDir, String model, String paramFileNameStr, File outfile, int numTopFacets) throws IOException {
        List<TfQuery> queries = QueryFileParser.loadQueries(queryFile);
        double[] avg = new double[metricNum()];
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

    @Override
    public double[] eval(List<AnnotatedFacet> facets, List<ScoredFacet> system, int numTopFacets) {
        double[] scores = new double[metricNum()];
        
        int i = 0;
        for(QueryFacetEvaluator evaluator : evaluators) {
            for(double score : evaluator.eval(facets, system, numTopFacets)) {
                scores[i++] = score;
            }
        }
        
        return scores;
    }

    public static double f1(double p, double r) {
        return p + r < Utility.epsilon ? 0 : 2 * p * r / (p + r);
    }

    @Override
    public int metricNum() {
        int num = 0;
        for(QueryFacetEvaluator evaluator : evaluators) {
            num += evaluator.metricNum();
        }
        return num;
    }

    

}
