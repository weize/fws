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
public interface QueryFacetEvaluator {
    
    // number of measures to evaluate
    int  metricNum();
    
    // evaluate for these measures for top system facets
    double[] eval(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets, int numTopFacets);
    
}
