/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.utility.TextProcessing;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class AnnotatedFacet implements Comparable<AnnotatedFacet> {

    public String description;
    public String fid;
    private int fidInt;
    public int rating;
    public List<String> terms;

    public AnnotatedFacet(int rating, String fid, String description) {
        this.rating = rating;
        this.description = description;
        this.fid = fid;
        this.fidInt = Integer.parseInt(fid);
        terms = new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%s", fid, rating, description, TextProcessing.join(terms, "|"));
    }

    public boolean isValid() {
        return rating > 0;
    }

    @Override
    public int compareTo(AnnotatedFacet other) {
        int cmp = other.rating - this.rating;
        return cmp == 0 ? this.fidInt - other.fidInt : cmp;
    }

    public void addTerm(String term) {
        terms.add(term);
    }
    
    public int size() {
        return terms.size();
    }
    
    public String get(int index) {
        return terms.get(index);
    }

    public ScoredFacet toScoredFacet() {
        ArrayList<ScoredItem> items = new ArrayList<>();
        for(String t:terms) {
            items.add(new ScoredItem(t, 0));
        }
        
        return new ScoredFacet(items, rating);
    }
    
    public Parameters toParameters( ) {
        Parameters facet = new Parameters();
        facet.put("rating", (long)(rating));
        facet.put("description", description);
        facet.put("terms", terms);
        return facet;
    }

}
