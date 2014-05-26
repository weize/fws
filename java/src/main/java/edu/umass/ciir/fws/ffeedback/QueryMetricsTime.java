/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author wkong
 */
public class QueryMetricsTime extends QueryMetrics {

    int time;

    public QueryMetricsTime(QueryMetrics qm, int time) {
        super(qm.qid, qm.valueStrs);
        this.time = time;
    }

    public QueryMetricsTime(String qid, String[] valueStrs, int time) {
        super(qid, valueStrs);
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

    public static void output(HashMap<String, List<QueryMetricsTime>> qmtMap, File outfile) throws IOException {
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
}
