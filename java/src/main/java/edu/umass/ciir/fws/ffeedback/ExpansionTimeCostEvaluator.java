/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.eval.FfeedbackTimeEstimator;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
import edu.umass.ciir.fws.types.TfFacetFeedbackParams;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Input: expansion file, eval file.
 *
 * @author wkong
 */
public class ExpansionTimeCostEvaluator {

    Parameters p;
    TreeMap<String, List<QueryMetricsTime>> expQmtMap; // qid-sid -> [(map, ndcg, ..., time), ...]
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
        // extract time cost evals
        expQmtMap = calcQueryMetricTimes(ffParam);
        // to average
        // each line all-all time metrics
        List<QueryMetricsTime> agvQmt = QueryMetricsTime.avgQmts(expQmtMap, avgQidSid);
        expQmtMap.put(avgQidSid, agvQmt);

        // to average between queries
        List<QueryMetricsTime> avgByQueryQmt = QueryMetricsTime.avgQmtsByQuery(expQmtMap, "all");
        
        // output
        File expansionTimeEvalFile = expansionDir.getExpansionTimeCostEvalFile(ffParam, expansionModel);
        File expansionTimeEvalAvgFile = expansionDir.getExpansionTimeCostEvalAvgFile(ffParam, expansionModel);
        File expansionTimeEvalQueryAvgFile = expansionDir.getExpansionTimeCostEvalQueryAvgFile(ffParam, expansionModel);
        
        Utility.infoOpen(expansionTimeEvalFile);
        QueryMetricsTime.output(expQmtMap, expansionTimeEvalFile);
        Utility.infoWritten(expansionTimeEvalFile);
        
        
        Utility.infoOpen(expansionTimeEvalAvgFile);
        QueryMetricsTime.outputAvg(expansionTimeEvalAvgFile, agvQmt);
        Utility.infoWritten(expansionTimeEvalAvgFile);
        
        Utility.infoOpen(expansionTimeEvalQueryAvgFile);
        QueryMetricsTime.outputAvg(expansionTimeEvalQueryAvgFile, avgByQueryQmt);
        Utility.infoWritten(expansionTimeEvalQueryAvgFile);

    }

    /**
     * form eval of expansions to : qid sid [(time, metrics)]
     * @param ffParam
     * @return
     * @throws IOException 
     */
    private TreeMap<String, List<QueryMetricsTime>> calcQueryMetricTimes(TfFacetFeedbackParams ffParam) throws IOException {
        

        File expansionFile = expansionDir.getExpansionFile(ffParam, expansionModel);
        File expansionEvalFile = expansionDir.getExpansionEvalFile(ffParam, expansionModel);

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
