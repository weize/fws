/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.utility.TextProcessing;
import java.util.ArrayList;

/**
 * feedback terms from a same facet.
 *
 * @author wkong
 */
public class FeedbackList {

    int fid;
    int index; // index of the facet in the shown facet list
    ArrayList<FeedbackTerm> terms;

    public FeedbackList(int facetId, int index) {
        this.fid = facetId;
        this.index = index;
        terms = new ArrayList<>();
    }

    public void add(FeedbackTerm term) {
        terms.add(term);
    }

    public int size() {
        return terms.size();
    }

    @Override
    public String toString() {
        return fid + "\t" + index + "\t" + TextProcessing.join(terms, "|");
    }
}
