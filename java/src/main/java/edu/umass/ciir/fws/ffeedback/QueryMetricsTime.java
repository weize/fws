/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author wkong
 */
public class QueryMetricsTime extends QueryMetrics {

    

    public int time;

    public QueryMetricsTime(QueryMetrics qm, int time) {
        super(qm.qid, qm.valueStrs);
        this.time = time;
    }

    public QueryMetricsTime(String qid, String[] valueStrs, int time) {
        super(qid, valueStrs);
        this.time = time;
    }

    public QueryMetricsTime(String qid, double[] values, int time) {
        super(qid, values);
        this.time = time;
    }

    /**
     * *
     * time:map,ndcg,...
     *
     * @return
     */
    public String getTimeMetricsString() {
        return time + ":" + TextProcessing.join(valueStrs, ",");
    }

    public static QueryMetricsTime parse(String qid, String qmtStr) {
        String [] elems = qmtStr.split(":");
        int time = Integer.parseInt(elems[0]);
        String [] scores = elems[1].split(",");
        QueryMetrics qm = new QueryMetrics(qid,scores);
        return new QueryMetricsTime(qm, time);
    }
    
    public static void outputAvg(File file, List<QueryMetricsTime> avgQmts) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (QueryMetricsTime qmt : avgQmts) {
            writer.write(String.format("%s\t%d\t%s\n", qmt.qid, qmt.time, TextProcessing.join(qmt.valueStrs, "\t")));
        }
        writer.close();
    }
    
    public static void output(Map<String, List<QueryMetricsTime>> qmtMap, File outfile) throws IOException {
        BufferedWriter writer = Utility.getWriter(outfile);
        for (String qidSid : qmtMap.keySet()) {
            writer.write(qidSid + "\t");
            boolean first = true;
            List<QueryMetricsTime> list = qmtMap.get(qidSid);
            Collections.sort(list, new Comparator<QueryMetricsTime>() {

                @Override
                public int compare(QueryMetricsTime o1, QueryMetricsTime o2) {
                    return o1.time - o2.time;
                }
            });

            for (QueryMetricsTime qmt : list) {
                if (first) {
                    writer.write(qmt.getTimeMetricsString());
                    first = false;
                } else {
                    writer.write("|" + qmt.getTimeMetricsString());
                }
            }
            writer.newLine();
        }
        writer.close();
    }

    /**
     * not tested
     * @param file
     * @return
     * @throws IOException 
     */
    public static Map<String, List<QueryMetricsTime>> loadAsMap(File file) throws IOException {
        HashMap<String, List<QueryMetricsTime>> map = new HashMap<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            String qidSid = elems[0];
            String qmtList = elems[1];

            ArrayList<QueryMetricsTime> list = new ArrayList<>();
            for (String qmtStr : qmtList.split("\\|")) {
                String[] elems2 = qmtStr.split(":");
                int time = Integer.parseInt(elems2[0]);
                String[] valueStrs = elems2[1].split(",");
                list.add(new QueryMetricsTime(qidSid, valueStrs, time));
            }
            map.put(qidSid, list);
        }
        reader.close();
        return map;
    }

    

}
