/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.qperformance;

/**
 *
 * @author wkong
 */
public class QueryPerformanceFeatures extends QueryFeatures {

    double score; // performance
    
    public QueryPerformanceFeatures(String qid, String query, double score) {
        super(qid, query);
        this.score = score;
    }
    
}
