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
        return "fws extract gain-at-time --tcevalFile=<tcevalFile> --output=<outfile> --time=<timeMeasure>";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        File tcevalFile = new File(p.getString("tcevalFile"));
        File outfile = new File(p.getString("outfile"));
        int time = new Long(p.getLong("time")).intValue();

        TreeMap<String, TreeMap<String, QueryMetricsTime>> qmts = new TreeMap<>();
        BufferedReader reader = Utility.getReader(tcevalFile);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            String qidSid = elems[0];
            String[] qmtStrs = elems[1].split("\\|");
            if (qidSid.equals("all-all") || qidSid.equals("all")) {
                continue;
            }
            QueryMetricsTime qmt = getQmtAtTime(qidSid, qmtStrs, time);

            String qid = qidSid.split("-")[0];
            String sid = qidSid.split("-")[1];
            if (!qmts.containsKey(qid)) {
                qmts.put(qid, new TreeMap<String, QueryMetricsTime>());
            }
            qmts.get(qid).put(sid, qmt);
        }
        reader.close();
        
        TreeMap<String, QueryMetricsTime> qmtsAvgByQuery = new TreeMap<>();
        for(String qid : qmts.keySet()) {
            TreeMap<String, QueryMetricsTime> cur = qmts.get(qid);
            double [] avg = new double[cur.get(cur.firstKey()).values.length];
            for(String sid: cur.keySet()) {
                Utility.add(avg, cur.get(sid).values);
            }
            Utility.avg(avg, cur.size());
            qmtsAvgByQuery.put(qid, new QueryMetricsTime(qid, avg, time));
        }
        output(qmtsAvgByQuery, outfile);
    }

    private QueryMetricsTime getQmtAtTime(String qidSid, String[] qmtStrs, long time) {
        QueryMetricsTime qmt = QueryMetricsTime.parse(qidSid, qmtStrs[0]);

        for (String qmtStr : qmtStrs) {
            QueryMetricsTime cur = QueryMetricsTime.parse(qidSid, qmtStr);
            if (cur.time <= time) {
                qmt = cur;
            } else {
                break;
            }
        }

        return qmt;
    }

    private void output( TreeMap<String, QueryMetricsTime> qmts, File outfile) throws IOException {
        BufferedWriter writer = Utility.getWriter(outfile);
        for(QueryMetricsTime qmt : qmts.values()) {
            writer.write(String.format("%s\t%d\t%s\n", qmt.qid, qmt.time, TextProcessing.join(qmt.valueStrs, "\t")));
        }
        writer.close();
    }

}
