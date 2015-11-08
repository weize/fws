/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.demo.search.GalagoSearchEngine;
import edu.umass.ciir.fws.demo.search.SearchEngine;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class FacetRefinerFactory {

    public static FacetRefiner instance(Parameters p, SearchEngine searchEngine) {
        String facetModel = p.getString("facetModel");
        if (facetModel.equals("gmj")) {
            GalagoSearchEngine galago;
            if (!searchEngine.getClass().isInstance(GalagoSearchEngine.class)) {
                galago = (GalagoSearchEngine) searchEngine;
            } else {
                galago = new GalagoSearchEngine(p);
            }
            return new GmFacetRefiner(p, galago);
        } else {
            return null;
        }
    }

}
