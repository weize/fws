/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.query.QueryTopicSubtopicMap;
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
 *
 * @author wkong
 */
public class FilterTimeCostEvaluation extends AppFunction {

    Parameters p;
    TreeMap<String, List<QueryMetricsTime>> expQmtMap; // qid-sid -> [(map, ndcg, ..., time), ...]

    @Override
    public String getName() {
        return "filter-tc-eval";
    }

    @Override
    public String getHelpString() {
        return "fws extract filter-tc-eval --tcevalFile=<tcevalFile> --qidSidFile=<qidSidFile> --outfile=<outfile>\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        File tcevalFile = new File(p.getString("tcevalFile"));
        File qidSidFile = new File(p.getString("qidSidFile"));
        QueryTopicSubtopicMap queryMap = new QueryTopicSubtopicMap(qidSidFile);
        File outfile = new File(p.getString("outfile"));
        
        // qid sid
        TreeMap<String, List<QueryMetricsTime>> all = QueryMetricsTime.loadTimeCostEvalFile(tcevalFile);
        TreeMap<String, List<QueryMetricsTime>> qmts =  new TreeMap<>();
        for(String qidSid : all.keySet()) {
            String qid = qidSid.split("-")[0];
            String sid = qidSid.split("-")[1];
            if (queryMap.hasSubtopic(qid, sid)) {
                qmts.put(qidSid, all.get(qidSid));
            }
        }
        
        List<QueryMetricsTime> avg = QueryMetricsTime.avgQmtsByQuery(qmts, "all");
        
        QueryMetricsTime.outputAvg(outfile, avg);
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

    private void output(TreeMap<String, QueryMetricsTime> qmts, File outfile) throws IOException {
        BufferedWriter writer = Utility.getWriter(outfile);
        for (QueryMetricsTime qmt : qmts.values()) {
            writer.write(String.format("%s\t%d\t%s\n", qmt.qid, qmt.time, TextProcessing.join(qmt.valueStrs, "\t")));
        }
        writer.close();
    }

}
