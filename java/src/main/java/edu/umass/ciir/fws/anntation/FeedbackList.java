/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.utility.TextProcessing;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * feedback terms from a same facet.
 *
 * @author wkong
 */
public class FeedbackList implements Comparable<FeedbackList>, Iterable<FeedbackTerm>{

    String fid;
    int index; // index of the facet in the shown facet list
    public ArrayList<FeedbackTerm> terms;

    public FeedbackList(String facetId, int index) {
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
    
    public void sortTerms() {
        Collections.sort(terms);
    }

    @Override
    public String toString() {
        return fid + "\t" + index + "\t" + TextProcessing.join(terms, "|");
    }

    @Override
    public int compareTo(FeedbackList other) {
        return this.index - other.index;
    }

    @Override
    public Iterator<FeedbackTerm> iterator() {
        return terms.iterator();
    }
}
