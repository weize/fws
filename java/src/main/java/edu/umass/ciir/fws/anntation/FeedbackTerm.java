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
public class FeedbackTerm implements Comparable<FeedbackTerm> {

    String term;
    int fidx; // index of the facet, of which the term apperas in
    int tidx; // index of the term in the shown facet

    public FeedbackTerm(String term, int fidx, int tidx) {
        this.term = term;
        this.fidx = fidx;
        this.tidx = tidx;
    }

    @Override
    public String toString() {
        return String.format("%d-%d:%s", fidx, tidx, term);
    }

    /**
     * Sort by fidx first, then tidx.
     * @param other
     * @return 
     */
    @Override
    public int compareTo(FeedbackTerm other) {
        int fdiff = this.fidx - other.fidx;
        return fdiff == 0 ? this.tidx - other.tidx : fdiff;
    }
}
