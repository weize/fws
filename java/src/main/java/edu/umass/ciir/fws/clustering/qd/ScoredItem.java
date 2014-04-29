/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

import org.lemurproject.galago.tupleflow.Utility;

/**
 *
 * @author wkong
 */
public class ScoredItem implements Comparable<ScoredItem> {

    public String item;
    public double score;

    public ScoredItem(String item, double score) {
        this.item = item;
        this.score = score;
    }

    public ScoredItem(String scoredItemStr) {
        String[] fields = scoredItemStr.split(":");
        item = fields[0];
        score = Double.parseDouble(fields[1]);
    }

    @Override
    public int compareTo(ScoredItem other) {
        return Utility.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return item + ":" + score;
    }
}
