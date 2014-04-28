/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.qd;

/**
 *
 * @author wkong
 */
public class FacetTerm {
    public String term;
    public double score;
    
    public FacetTerm(String term, double score) {
        this.term = term;
        this.score = score;
    }
    
}
