/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.clustering.ScoredFacet;

/**
 *
 * @author wkong
 */
public class AnnotatedFacet extends ScoredFacet {

    String description;

    public AnnotatedFacet(double score, String description) {
        super(score);
        this.description = description;
    }

    @Override
    public String toString() {
        return description +"\t" + super.toFacetString();
    }

}
