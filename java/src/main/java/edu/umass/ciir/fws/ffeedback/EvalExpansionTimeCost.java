/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.eval.FfeedbackTimeEstimator;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
import static edu.umass.ciir.fws.ffeedback.RunExpasions.setParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Input: expansion file, eval file.
 *
 * @author wkong
 */
public class EvalExpansionTimeCost extends AppFunction {

    @Override
    public String getName() {
        return "eval-expansion-time-cost";
    }

    @Override
    public String getHelpString() {
        return "fws " + getName() + " config.json --expansionSource=<> --expansionModel=<>\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        setParameters(p);
        String model = p.getString("expansionModel");
        File expansionFile = new File(p.getString("expansionFile"));
        File expansionEvalFile = new File(p.getString("expansionEvalFile"));
        File expansionTimeEvalFile = new File(expansionEvalFile + ".tceval"); // time cost eval
        File sdmSevalFile = new File(p.getString("sdmSeval"));

        // baseline, no expansion, time cost = 0
        HashMap<String, QueryMetrics> sdmQmMap = TrecEvaluator.loadQueryMetricsMap(sdmSevalFile, true);
        List<QueryMetrics> expQms = TrecEvaluator.loadQueryMetricsList(expansionEvalFile, true);
        // qid-model-expId
        HashMap<String, QuerySubtopicExpansion> qseMap = QuerySubtopicExpansion.loadQueryExpansionAsMap(expansionFile, model);

        // qid-sid -> [(map, ndcg, ..., time), ...]
        HashMap<String, List<QueryMetricsTime>> expQmts = new HashMap<>();
        for (QueryMetrics expQm : expQms) {
            String[] qidSidModelExpId = expQm.qid.split("-");
            String qid = qidSidModelExpId[0];
            String sid = qidSidModelExpId[1];
            String curModel = qidSidModelExpId[2];
            long expId = Long.parseLong(qidSidModelExpId[3]);

            if (curModel.equals(model)) {
                String qidSid = qid + "-" + sid;

                if (!expQmts.containsKey(qidSid)) {
                    ArrayList<QueryMetricsTime> list = new ArrayList<>();
                    QueryMetrics sdmQm = sdmQmMap.get(qidSid);
                    int time = 0; // no feedback, zero time cost
                    list.add(new QueryMetricsTime(sdmQm, time));
                    expQmts.put(qidSid, list);
                }
                
                QuerySubtopicExpansion qes = qseMap.get(QuerySubtopicExpansion.toId(qid, sid, model, expId));
                FacetFeedback ffbk = FacetFeedback.parseFromExpansionString(qes.expansion);
                int time = FfeedbackTimeEstimator.time(ffbk);
                QueryMetricsTime qmt = new QueryMetricsTime(expQm, time);
                expQmts.get(qidSid).add(qmt);
            }

        }

        QueryMetricsTime.output(expQmts, expansionTimeEvalFile);
        Utility.infoWritten(expansionTimeEvalFile);

    }
}
