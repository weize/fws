/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import java.util.List;

/**
 *
 * @author wkong
 */
public class QueryFacetEvaluator {
    int numTopFacets;
    List<ScoredFacet> sysFacets; // system
    List<AnnotatedFacet> annFacets; // annotators
    
    
    protected void loadFacets(FacetAnnotation afacets, List<ScoredFacet> sfacets) {
         // only using top n sysFacets
        sysFacets = sfacets.subList(0, Math.min(sfacets.size(), numTopFacets));
        annFacets = afacets.facets;
    }
    
}
