/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.ffeedback.*;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.TreeMap;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Input: tceval file ,line : qid-sid [time:metric, ...]
 *
 * @author wkong
 */
public class ExtractGainAtTime extends AppFunction {

    Parameters p;
    TreeMap<String, List<QueryMetricsTime>> expQmtMap; // qid-sid -> [(map, ndcg, ..., time), ...]

    @Override
    public String getName() {
        return "extract-gain-at-time";
    }

    @Override
    public String getHelpString() {
        return "fws extract-gain-at-time --tcevalFile=<tcevalFile> --outfile=<outfile> --time=<timeMeasure>";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        File tcevalFile = new File(p.getString("tcevalFile"));
        File outfile = new File(p.getString("outfile"));
        int time = new Long(p.getLong("time")).intValue();
        TreeMap<String, List<QueryMetricsTime>> qmts = QueryMetricsTime.loadTimeCostEvalFile(tcevalFile);
        TreeMap<String, QueryMetricsTime> qmtsAvgByQuery = extract(qmts, time);
        output(qmtsAvgByQuery, outfile);
    }

    public static TreeMap<String, QueryMetricsTime> extract(TreeMap<String, List<QueryMetricsTime>> oriQmts, int time) {

        TreeMap<String, TreeMap<String, QueryMetricsTime>> qmts = new TreeMap<>();
        for (String qidSid : oriQmts.keySet()) {
            if (qidSid.equals("all-all") || qidSid.equals("all")) {
                continue;
            }
            QueryMetricsTime qmt = getQmtAtTime(oriQmts.get(qidSid), time);

            String qid = qidSid.split("-")[0];
            String sid = qidSid.split("-")[1];
            if (!qmts.containsKey(qid)) {
                qmts.put(qid, new TreeMap<String, QueryMetricsTime>());
            }
            qmts.get(qid).put(sid, qmt);
        }

        TreeMap<String, QueryMetricsTime> qmtsAvgByQuery = new TreeMap<>();
        for (String qid : qmts.keySet()) {
            TreeMap<String, QueryMetricsTime> cur = qmts.get(qid);
            double[] avg = new double[cur.get(cur.firstKey()).values.length];
            for (String sid : cur.keySet()) {
                Utility.add(avg, cur.get(sid).values);
            }
            Utility.avg(avg, cur.size());
            qmtsAvgByQuery.put(qid, new QueryMetricsTime(qid, avg, time));
        }
        return qmtsAvgByQuery;

    }

    private static QueryMetricsTime getQmtAtTime(List<QueryMetricsTime> list, long time) {
        QueryMetricsTime qmt = list.get(0);

        for (QueryMetricsTime cur : list) {
            if (cur.time <= time) {
                qmt = cur;
            } else {
                break;
            }
        }

        return qmt;
    }

    public static void output(TreeMap<String, QueryMetricsTime> qmts, File outfile) throws IOException {
        BufferedWriter writer = Utility.getWriter(outfile);
        for (QueryMetricsTime qmt : qmts.values()) {
            writer.write(String.format("%s\t%d\t%s\n", qmt.qid, qmt.time, TextProcessing.join(qmt.valueStrs, "\t")));
        }
        writer.close();
    }

}
