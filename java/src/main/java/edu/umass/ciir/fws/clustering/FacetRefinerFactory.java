/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.clustering;

import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class FacetRefinerFactory {

    public static FacetRefiner instance(Parameters p) {
        String facetModel = p.getString("facetModel");
        if (facetModel.equals("gmj")) {
            return new GmFacetRefiner(p);
        } else {
            return null;
        }
    }
    
}
