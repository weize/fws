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
import java.util.TreeMap;

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
        String[] elems = qmtStr.split(":");
        int time = Integer.parseInt(elems[0]);
        String[] scores = elems[1].split(",");
        QueryMetrics qm = new QueryMetrics(qid, scores);
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

    public static TreeMap<String, List<QueryMetricsTime>> loadTimeCostEvalFile(File file) throws IOException {
        TreeMap<String, List<QueryMetricsTime>> qmts = new TreeMap<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            String qidSid = elems[0];
            String[] qmtStrs = elems[1].split("\\|");
            ArrayList<QueryMetricsTime> list = new ArrayList<>();

            for (String qmtStr: qmtStrs) {
                QueryMetricsTime cur = QueryMetricsTime.parse(qidSid, qmtStr);
                list.add(cur);
            }
            qmts.put(qidSid, list);
        }
        reader.close();
        return qmts;
    }
    
    /**
     * average across queries
     * @param expQmtMap
     * @param qidSid
     * @return 
     */
    public static List<QueryMetricsTime> avgQmts(TreeMap<String, List<QueryMetricsTime>> expQmtMap, String qidSid) {
        ArrayList<QueryMetricsTime> avgQmt = new ArrayList<>();
        // map to lists
        List<QueryMetricsTime>[] expQmtLists = (List<QueryMetricsTime>[]) expQmtMap.values().toArray(new List<?>[0]);

        int metricSize = expQmtLists[0].get(0).values.length;
        // set point to each list
        int[] ii = new int[expQmtLists.length];
        for (int i = 0; i < ii.length; i++) {
            ii[i] = -1;
        }

        while (true) {
            int time = findNextMinTimeCost(expQmtLists, ii);
            if (time < 0) {
                break;
            }

            // add values for that time
            double[] values = new double[metricSize];
            for (int i = 0; i < expQmtLists.length; i++) {
                List<QueryMetricsTime> qmts = expQmtLists[i];
                int indexNext = ii[i] + 1;
                if (indexNext < qmts.size() && qmts.get(indexNext).time == time) {
                    ii[i]++; // move to next qmt
                }

                Utility.add(values, qmts.get(ii[i]).values);
            }
            Utility.avg(values, expQmtLists.length);
            avgQmt.add(new QueryMetricsTime(qidSid, values, time));
        }
        return avgQmt;
    }
    
    public static List<QueryMetricsTime> avgQmtsByQuery(TreeMap<String, List<QueryMetricsTime>> qmts, String avgName) throws IOException {
        // qid-> sid-> [QueryMetrics]
        HashMap<String, TreeMap<String, List<QueryMetricsTime>>> expQmtMapByQid = new HashMap<>();
        
        for (String qidSid : qmts.keySet()) {
            String [] elems= qidSid.split("-");
            String qid = elems[0];
            String sid = elems[1];
            
            if (!expQmtMapByQid.containsKey(qid)) {
                expQmtMapByQid.put(qid, new TreeMap<String,List<QueryMetricsTime>>());
            }
            
            expQmtMapByQid.get(qid).put(sid, qmts.get(qidSid));
        }
        
        TreeMap<String, List<QueryMetricsTime>> expAvgBySubtopicQmtMap = new TreeMap<>();
        for(String qid : expQmtMapByQid.keySet()) {
            // avg within a query
            List<QueryMetricsTime> avgQmt = avgQmts(expQmtMapByQid.get(qid), qid);
            expAvgBySubtopicQmtMap.put(qid, avgQmt);
        }
        
        //avg between queries
        List<QueryMetricsTime> avgByQueryQmt = avgQmts(expAvgBySubtopicQmtMap, avgName);
        
        return avgByQueryQmt;
    }
    
    public static int findNextMinTimeCost(List<QueryMetricsTime>[] expQmtLists, int[] ii) {
        int minTime = Integer.MAX_VALUE;
        boolean hasResult = false;

        for (int i = 0; i < expQmtLists.length; i++) {
            int index = ii[i] + 1;
            List<QueryMetricsTime> qmts = expQmtLists[i];
            if (index < qmts.size()) {
                int time = qmts.get(index).time;
                if (time < minTime) {
                    minTime = time;
                    hasResult = true;
                }
            }
        }
        return hasResult ? minTime : -1;
    }

}
