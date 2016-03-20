/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.eval.FfeedbackTimeEstimator;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
import static edu.umass.ciir.fws.ffeedback.QueryMetricsTime.avgQmts;
import edu.umass.ciir.fws.utility.TextProcessing;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class FeedbackPerQueryEval extends AppFunction {

    File sdmSevalFile;
    String expansionModel = "ffs";
    int time;
    TreeMap<String, List<QueryMetricsTime>> expQmtMap;

    @Override
    public String getName() {
        return "feedback-per-query-eval";
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        File expFile = new File(p.getString("expFile"));
        File expEvalFile = new File(p.getString("evalFile"));
        sdmSevalFile = new File(p.getString("sdmSeval"));
        time = (int) p.getLong("time");

        expQmtMap = calcQueryMetricTimes(expFile, expEvalFile);
        Map<String, QueryMetricsTime> queryQmt = collectQmtsByQuery(expQmtMap);
        for(String qid : queryQmt.keySet()) {
            System.out.println(String.format("%s\t%s", qid, TextProcessing.join(queryQmt.get(qid).valueStrs, "\t")));
        }
    }

    public Map<String, QueryMetricsTime> collectQmtsByQuery(Map<String, List<QueryMetricsTime>> qmts) throws IOException {
        // qid-> sid-> [QueryMetrics]
        HashMap<String, TreeMap<String, List<QueryMetricsTime>>> expQmtMapByQid = new HashMap<>();

        for (String qidSid : qmts.keySet()) {
            String[] elems = qidSid.split("-");
            String qid = elems[0];
            String sid = elems[1];

            if (!expQmtMapByQid.containsKey(qid)) {
                expQmtMapByQid.put(qid, new TreeMap<String, List<QueryMetricsTime>>());
            }

            expQmtMapByQid.get(qid).put(sid, qmts.get(qidSid));
        }

        TreeMap<String, QueryMetricsTime> expAvgBySubtopicQmtMap = new TreeMap<>();
        for (String qid : expQmtMapByQid.keySet()) {
            // avg within a query
            List<QueryMetricsTime> avgQmt = avgQmts(expQmtMapByQid.get(qid), qid);
            expAvgBySubtopicQmtMap.put(qid, findQmtAtTime(avgQmt, time));
        }

        return expAvgBySubtopicQmtMap;
    }

    private QueryMetricsTime findQmtAtTime(List<QueryMetricsTime> avgQmt, int time) {
        QueryMetricsTime res = null;
        for (QueryMetricsTime q : avgQmt) {
            if (q.time > time) {
                break;
            }
            res = q;
        }
        return res;
    }

    /**
     * form eval of expansions to : qid sid [(time, metrics)]
     *
     * @param ffParam
     * @return
     * @throws IOException
     */
    private TreeMap<String, List<QueryMetricsTime>> calcQueryMetricTimes(File expansionFile, File expansionEvalFile) throws IOException {

        // baseline, no expansion, time cost = 0
        TreeMap<String, QueryMetrics> sdmQmMap = TrecEvaluator.loadQueryMetricsMap(sdmSevalFile, true);
        List<QueryMetrics> expQms = TrecEvaluator.loadQueryMetricsList(expansionEvalFile, true);
        // qid-model-expId
        HashMap<String, QuerySubtopicExpansion> qseMap = QuerySubtopicExpansion.loadQueryExpansionAsMap(expansionFile, expansionModel);

        // qid-sid -> [(map, ndcg, ..., time), ...]
        TreeMap<String, List<QueryMetricsTime>> expQmts = new TreeMap<>();

        // no expansoins
        // in feedback, and in sdm eval(means has relevance judgements)
        for (String qidSid : sdmQmMap.keySet()) {
            ArrayList<QueryMetricsTime> list = new ArrayList<>();
            QueryMetrics sdmQm = sdmQmMap.get(qidSid);
            int time = 0; // no feedback, zero time cost
            list.add(new QueryMetricsTime(sdmQm, time));
            expQmts.put(qidSid, list);
        }

        for (QueryMetrics expQm : expQms) {
            String[] qidSidModelExpId = expQm.qid.split("-");
            String qid = qidSidModelExpId[0];
            String sid = qidSidModelExpId[1];
            String curModel = qidSidModelExpId[2];
            long expId = Long.parseLong(qidSidModelExpId[3]);

            if (curModel.equals(expansionModel)) {
                String qidSid = qid + "-" + sid;
                QuerySubtopicExpansion qes = qseMap.get(QuerySubtopicExpansion.toId(qid, sid, expansionModel, expId));
                FacetFeedback ffbk = FacetFeedback.parseFromExpansionString(qes.expansion);
                int time = FfeedbackTimeEstimator.time(ffbk);
                QueryMetricsTime qmt = new QueryMetricsTime(expQm, time);
                expQmts.get(qidSid).add(qmt);
            }

        }

        return expQmts;
    }
}
