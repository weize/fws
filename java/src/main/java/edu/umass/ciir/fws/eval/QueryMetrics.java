/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.utility.TextProcessing;

/**
 *
 * @author wkong
 */
public class QueryMetrics {
    public String qid;
    public String [] valueStrs; // metric values;
    public double [] values;

    QueryMetrics(String qid, String[] values) {
        this.qid = qid;
        this.valueStrs = values;
    }

    QueryMetrics(String qid, double[] values) {
        this.qid = qid;
        this.values = values;
    }
    
    @Override
    public String toString() {
        return qid +'\t' + TextProcessing.join(valueStrs, "\t");
    }
}
