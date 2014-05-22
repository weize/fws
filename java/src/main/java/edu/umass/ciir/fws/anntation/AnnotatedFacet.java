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

    public String description;
    public String fid;

    public AnnotatedFacet(double score, String fid, String description) {
        super(score);
        this.description = description;
        this.fid = fid;
    }

    @Override
    public String toString() {
        return fid + "\t" + description + "\t" + super.toFacetString();
    }
    
    public boolean isValid() {
        return score > 1.1;
    }

}
