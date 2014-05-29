/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author wkong
 */
public class QueryMetrics {
    public String qid;
    public String [] valueStrs; // metric values;
    public double [] values;

    public QueryMetrics(String qid, String[] valueStrs) {
        this.qid = qid;
        this.valueStrs = valueStrs;
        this.values = new double[valueStrs.length];
        for(int i = 0; i < valueStrs.length; i ++) {
            values[i] = Double.parseDouble(valueStrs[i]);
        }
    }

    public QueryMetrics(String qid, double[] values) {
        this.qid = qid;
        this.values = values;
        this.valueStrs = new String [values.length];
        for(int i = 0; i < values.length; i ++) {
            valueStrs[i] = String.format("%.4f", values[i]);
        }
    }
    
    @Override
    public String toString() {
        return qid +'\t' + TextProcessing.join(valueStrs, "\t");
    }
    
    public static void output(List<QueryMetrics> results,  File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for(QueryMetrics qm : results) {
            writer.write(qm.toString());
            writer.newLine();
        }
        writer.close();
    }
}
