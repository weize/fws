/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.experiment;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.clustering.FacetModelParamGenerator;
import edu.umass.ciir.fws.eval.FfeedbackTimeEstimator;
import edu.umass.ciir.fws.ffeedback.FacetFeedback;
import edu.umass.ciir.fws.ffeedback.FeedbackParameterGenerator;
import edu.umass.ciir.fws.ffeedback.QueryMetricsTime;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class CmpFacetsNumFeedbackTermOverTime extends AppFunction {

    @Override
    public String getName() {
        return "cmp-feedbacks-selected-term-num";
    }

    @Override
    public String getHelpString() {
        return "fws cmp-feedbacks-selected-term-num\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        List<String> facetSrcs = p.getAsList("facetSources");
        List<String> feedbackSrcs = p.getAsList("feedbackSources");
        String feedbackDir = p.getString("feedbackDir");
        FacetModelParamGenerator facetParamGen = new FacetModelParamGenerator(p);
        FeedbackParameterGenerator feedbackParamGen = new FeedbackParameterGenerator(p);

        for (String feedbackSrc : feedbackSrcs) {
            List<String> feedbackParams = feedbackParamGen.getParams(feedbackSrc);
            for (String feedbackParam : feedbackParams) {
                for (String facetSrc : facetSrcs) {
                    List<String> facetParams = facetParamGen.getParams(facetSrc);
                    for (String facetParam : facetParams) {
                        File feedbackFile = new File (Utility.getFeedbackFileName(feedbackDir, facetSrc, facetParam, feedbackSrc, feedbackParam));
                        File outfile = new File(geOutFileName("../exp/data/cmp-facet", facetSrc, facetParam, feedbackSrc, feedbackParam));
                        process(feedbackFile, outfile);
                    }
                }
            }
        }
    }
    
    public static String geOutFileName(String dir, String facetSource, String facetParams,
            String feedbackSource, String feedbackParams) {
        facetParams = Utility.parametersToFileNameString(facetParams);
        feedbackParams = Utility.parametersToFileNameString(feedbackParams);
        String facetName = facetParams.isEmpty() ? String.format("%s", facetSource)
                : String.format("%s.%s", facetSource, facetParams);

        String feedbackName = feedbackParams.isEmpty() ? String.format("%s", feedbackSource)
                : String.format("%s.%s", feedbackSource, feedbackParams);
        String name = String.format("%s.%s.selectTermNum", facetName, feedbackName);
        return Utility.getFileName(dir, name);
    }

    private void process(File feedbackFile, File outfile) throws IOException {
        HashMap<String, FacetFeedback> feedbacks = FacetFeedback.loadGroupByQidSid(feedbackFile);
        TreeMap<String, List<QueryMetricsTime>> qmts = new TreeMap<>();
        for (String qidSid : feedbacks.keySet()) {
            FacetFeedback feedback = feedbacks.get(qidSid);
            ArrayList<QueryMetricsTime> qmtList = new ArrayList<>();

            QueryMetricsTime first = new QueryMetricsTime(qidSid, new double[]{0}, 0);
            qmtList.add(first);
            
            ArrayList<FeedbackTerm> curTerms = new ArrayList<>();
            double termSelected = 0;
            for (FeedbackTerm t : feedback.terms) {
                curTerms.add(t);
                termSelected += 1;
                FacetFeedback cur = new FacetFeedback(qidSid, curTerms);
                int time = FfeedbackTimeEstimator.time(cur);
                QueryMetricsTime qmt = new QueryMetricsTime(qidSid, new double[]{termSelected}, time);
                qmtList.add(qmt);
            }
            qmts.put(qidSid, qmtList);
        }
        List<QueryMetricsTime> avgByQuery = QueryMetricsTime.avgQmtsByQuery(qmts, "all");
        QueryMetricsTime.outputAvg(outfile, avgByQuery);
        Utility.infoWritten(outfile);
    }

}
