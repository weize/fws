/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.demo;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.types.TfQuery;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class Demo {
    
    FWSEngine facetGenerator;

    public Demo(Parameters p) {
        facetGenerator = FWSEngineFactory.instance(p);
    }

    public List<ScoredFacet> runExtraction(TfQuery query, Parameters p) {
        return facetGenerator.generateFacets(query);
    }
    
}
