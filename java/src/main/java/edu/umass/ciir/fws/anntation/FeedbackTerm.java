/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.anntation;

/**
 *
 * @author wkong
 */
public class FeedbackTerm {
    String term;
    int index; // index of the term in the shown facet
    
    public FeedbackTerm(String term, int index) {
        this.term = term;
        this.index = index;
    }
    
    @Override
    public String toString() {
        return term+"@"+index;
    }
}
