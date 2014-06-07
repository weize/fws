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
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
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
        TreeMap<String, List<QueryMetricsTime>> expQmtMap = calcQueryMetricTimes(p);

        // to average
        String avgQidSid = "all-all";
        List<QueryMetricsTime> agvQmt = QueryMetricsTime.avgQmts(expQmtMap, avgQidSid);
        expQmtMap.put(avgQidSid, agvQmt);

        File expansionEvalFile = new File(p.getString("expansionEvalFile"));
        File expansionTimeEvalFile = new File(expansionEvalFile + ".tceval"); // time cost eval
        File expansionTimeEvalAvgFile = new File(expansionEvalFile + ".tceval.avg"); // time cost eval
        QueryMetricsTime.outputAvg(expansionTimeEvalAvgFile, agvQmt);
        QueryMetricsTime.output(expQmtMap, expansionTimeEvalFile);
        Utility.infoWritten(expansionTimeEvalFile);
        Utility.infoWritten(expansionTimeEvalAvgFile);

    }

    private TreeMap<String, List<QueryMetricsTime>> calcQueryMetricTimes(Parameters p) throws IOException {
        String model = p.getString("expansionModel");
        File expansionFile = new File(p.getString("expansionFile"));
        File expansionEvalFile = new File(p.getString("expansionEvalFile"));
        File feedbackFile = new File(p.getString("feedbackFile"));

        File sdmSevalFile = new File(p.getString("sdmSeval"));

        HashSet<String> qidSidSet = FacetFeedback.loadFeedbackQidSidSet(feedbackFile);
        // baseline, no expansion, time cost = 0
        TreeMap<String, QueryMetrics> sdmQmMap = TrecEvaluator.loadQueryMetricsMap(sdmSevalFile, true);
        List<QueryMetrics> expQms = TrecEvaluator.loadQueryMetricsList(expansionEvalFile, true);
        // qid-model-expId
        HashMap<String, QuerySubtopicExpansion> qseMap = QuerySubtopicExpansion.loadQueryExpansionAsMap(expansionFile, model);

        // qid-sid -> [(map, ndcg, ..., time), ...]
        TreeMap<String, List<QueryMetricsTime>> expQmts = new TreeMap<>();

        // no expansoins
        // in feedback, and in sdm eval(means has relevance judgements)
        for (String qidSid : qidSidSet) {
            if (sdmQmMap.containsKey(qidSid)) {
                ArrayList<QueryMetricsTime> list = new ArrayList<>();
                QueryMetrics sdmQm = sdmQmMap.get(qidSid);
                int time = 0; // no feedback, zero time cost
                list.add(new QueryMetricsTime(sdmQm, time));
                expQmts.put(qidSid, list);
            }
        }

        for (QueryMetrics expQm : expQms) {
            String[] qidSidModelExpId = expQm.qid.split("-");
            String qid = qidSidModelExpId[0];
            String sid = qidSidModelExpId[1];
            String curModel = qidSidModelExpId[2];
            long expId = Long.parseLong(qidSidModelExpId[3]);

            if (curModel.equals(model)) {
                String qidSid = qid + "-" + sid;
                QuerySubtopicExpansion qes = qseMap.get(QuerySubtopicExpansion.toId(qid, sid, model, expId));
                FacetFeedback ffbk = FacetFeedback.parseFromExpansionString(qes.expansion);
                int time = FfeedbackTimeEstimator.time(ffbk);
                QueryMetricsTime qmt = new QueryMetricsTime(expQm, time);
                expQmts.get(qidSid).add(qmt);
            }

        }

        return expQmts;
    }
    
}
