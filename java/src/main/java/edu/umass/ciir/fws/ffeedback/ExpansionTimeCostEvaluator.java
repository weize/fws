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
import edu.umass.ciir.fws.types.TfFacetFeedbackParams;
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
public class ExpansionTimeCostEvaluator {

    Parameters p;
    TreeMap<String, List<QueryMetricsTime>> expQmtMap;
    final static String avgQidSid = "all-all";
    String allFeedbackDir;
    File sdmSevalFile;
    String expansionModel;
    ExpansionDirectory expansionDir;

    public ExpansionTimeCostEvaluator(Parameters p) {
        this.p = p;
        allFeedbackDir = p.getString("feedbackDir");
        sdmSevalFile = new File(p.getString("sdmSeval"));
        expansionModel = p.getString("expansionModel");
        expansionDir = new ExpansionDirectory(p);
    }

    public void run(TfFacetFeedbackParams ffParam) throws IOException {
        expQmtMap = calcQueryMetricTimes(ffParam);

        // to average
        List<QueryMetricsTime> agvQmt = avgQmts(expQmtMap, avgQidSid);
        expQmtMap.put(avgQidSid, agvQmt);

        
        // output
        File expansionTimeEvalFile = expansionDir.getExpansionTimeCostEvalFile(ffParam, expansionModel);
        File expansionTimeEvalAvgFile = expansionDir.getExpansionTimeCostEvalAvgFile(ffParam, expansionModel);
        
        Utility.infoOpen(expansionTimeEvalFile);
        QueryMetricsTime.output(expQmtMap, expansionTimeEvalFile);
        Utility.infoWritten(expansionTimeEvalFile);
        
        
        Utility.infoOpen(expansionTimeEvalAvgFile);
        QueryMetricsTime.outputAvg(expansionTimeEvalAvgFile, agvQmt);
        Utility.infoWritten(expansionTimeEvalAvgFile);

    }

    private TreeMap<String, List<QueryMetricsTime>> calcQueryMetricTimes(TfFacetFeedbackParams ffParam) throws IOException {
        

        File expansionFile = expansionDir.getExpansionFile(ffParam.facetSource, expansionModel);
        File expansionEvalFile = expansionDir.getExpansionEvalFile(ffParam.facetSource, expansionModel);

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

    private List<QueryMetricsTime> avgQmts(TreeMap<String, List<QueryMetricsTime>> expQmtMap, String qidSid) {
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

    private int findNextMinTimeCost(List<QueryMetricsTime>[] expQmtLists, int[] ii) {
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
