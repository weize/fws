/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author wkong
 */
public class QueryMetrics {

    public String qid;
    public String[] valueStrs; // metric values;
    public double[] values;

    public QueryMetrics(String qid, String[] valueStrs) {
        this.qid = qid;
        this.valueStrs = valueStrs;
        this.values = new double[valueStrs.length];
        for (int i = 0; i < valueStrs.length; i++) {
            values[i] = Double.parseDouble(valueStrs[i]);
        }
    }

    public QueryMetrics(String qid, double[] values) {
        this.qid = qid;
        this.values = values;
        this.valueStrs = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            valueStrs[i] = String.format("%.4f", values[i]);
        }
    }

    @Override
    public String toString() {
        return qid + '\t' + TextProcessing.join(valueStrs, "\t");
    }

    public static void output(List<QueryMetrics> results, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (QueryMetrics qm : results) {
            writer.write(qm.toString());
            writer.newLine();
        }
        writer.close();
    }

    public static double getAvgScore(File evalFile, int metricIdx) throws IOException {
        BufferedReader reader = Utility.getReader(evalFile);
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().startsWith("#")) {
                QueryMetrics qm = QueryMetrics.parse(line);
                if (qm.qid.equals("all")) {
                    return qm.values[metricIdx];
                }
            }
        }
        reader.close();
        Utility.info("close file: "+evalFile.getAbsolutePath());
        throw new IOException("cannot find avg score for index " + metricIdx + " in " + evalFile);
    }

    private static QueryMetrics parse(String line) {
        String[] elems = line.split("\t");
        String qid = elems[0];
        String[] valueStrs = Arrays.copyOfRange(elems, 1, elems.length);
        return new QueryMetrics(qid, valueStrs);
    }
}
