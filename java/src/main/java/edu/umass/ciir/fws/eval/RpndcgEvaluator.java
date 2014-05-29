/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import java.util.List;

/**
 *
 * @author wkong
 */
public class RpndcgEvaluator extends QueryFacetEvaluator {
    
    private static class PrFacet {
        double precision;
        List<String> items;
        double recall;
        boolean mapped;
        double rating;
        
    }
    
     /**
     *
     * @param afacets sysFacets from annotator
     * @param sfacets sysFacets from system
     */
    public void eval(FacetAnnotation afacets, List<ScoredFacet> sfacets) {
        loadFacets(afacets, sfacets);
        
        // create 
        
        // map system sysFacets to annotator sysFacets
        for (ScoredFacet facet : sysFacets) {
            mapToAnnotatorFacet(facet);
        }
    }

    private void mapToAnnotatorFacet(ScoredFacet facet) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
