/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback.oracle;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
import edu.umass.ciir.fws.ffeedback.FacetFeedback;
import edu.umass.ciir.fws.ffeedback.QueryExpansion;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
public class ExtractOracleFeedbacks extends AppFunction {

    @Override
    public String getName() {
        return "extract-oracle-feedbacks";
    }

    @Override
    public String getHelpString() {
        return "fws " + getName() + " config.json";
    }

    static class Improvement extends FeedbackTerm {

        long termId;
        double[] metricImprvs;

        public Improvement(long termId, FeedbackTerm term, double[] metricImprvs) {
            super(term.term, term.fidx, term.tidx);
            this.termId = termId;
            this.metricImprvs = metricImprvs;
        }

    }

    private TreeMap<String, ArrayList<Improvement>> calcImprovements(File expansionEvalFile, File sdmSevalFile, File expansionFile, String model) throws IOException {
        HashMap<String, FeedbackTerm> expTermMap = QueryExpansion.loadExpansionTermsAsMap(expansionFile, model);
        HashMap<String, QueryMetrics> sdmQms = TrecEvaluator.loadQueryMetricsMap(sdmSevalFile, true);
        List<QueryMetrics> expQms = TrecEvaluator.loadQueryMetricsList(expansionEvalFile, true);

        TreeMap<String, ArrayList<Improvement>> subtopicImprvs = new TreeMap<>();
        // qid-sid  
        for (QueryMetrics qm : expQms) {
            double[] improvement = new double[qm.values.length];
            String[] qidSidModelExpId = qm.qid.split("-");
            String qid = qidSidModelExpId[0];
            String sid = qidSidModelExpId[1];
            String curModel = qidSidModelExpId[2];
            long expId = Long.parseLong(qidSidModelExpId[3]);

            if (curModel.equals(model)) {
                String qidSid = qid + "-" + sid;
                QueryMetrics sdmQm = sdmQms.get(qidSid);
                for (int i = 0; i < improvement.length; i++) {
                    improvement[i] = qm.values[i] - sdmQm.values[i];
                }

                FeedbackTerm term = expTermMap.get(QueryExpansion.toName(qid,model,expId));
                Improvement imprv = new Improvement(expId, term, improvement);

                if (subtopicImprvs.containsKey(qidSid)) {
                    subtopicImprvs.get(qidSid).add(imprv);
                } else {
                    ArrayList<Improvement> list = new ArrayList<>();
                    list.add(imprv);
                    subtopicImprvs.put(qidSid, list);
                }
            }

        }
        return subtopicImprvs;
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {

        File expansionFile = new File(p.getString("expansionFile"));
        String model = p.getString("expansionModel");
        File sdmSevalFile = new File(p.getString("sdmSeval"));
        File expansionEvalFile = new File(p.getString("expansionEvalFile"));
        File imprvFile = new File(p.getString("oracleExpansionEvalImproveFile"));
        String feedbackDir = p.getString("oracleFeedbackDir");
        List<Double> threshoulds = p.getAsList("oracleFeedbackImprvThresholds", Double.class);

        TreeMap<String, ArrayList<Improvement>> subtopicImprvs = calcImprovements(
                expansionEvalFile, sdmSevalFile, expansionFile, model);

        outputImprovementFile(subtopicImprvs, imprvFile);

        for (double threshold : threshoulds) {
            outputFeedbackTerms(subtopicImprvs, threshold, feedbackDir);
        }

    }

    private void outputFeedbackTerms(TreeMap<String, ArrayList<Improvement>> subtopicImprvs, double threshold, String feedbackDir) throws IOException {
        File outfile = new File(Utility.getOracleFeedbackFile(feedbackDir, threshold));
        BufferedWriter writer = Utility.getWriter(outfile);
        for (String qidSid : subtopicImprvs.keySet()) {
            ArrayList<FeedbackTerm> selected = new ArrayList<>();
            for (Improvement imprv : subtopicImprvs.get(qidSid)) {
                if (imprv.metricImprvs[0] >= threshold) {
                    selected.add(imprv);
                }
            }
            FacetFeedback ff = new FacetFeedback(qidSid, selected);
            writer.write(ff.toString());
            writer.newLine();
        }
        writer.close();
        Utility.infoWritten(outfile);
    }

    private void outputImprovementFile(TreeMap<String, ArrayList<Improvement>> subtopicImprvs, File imprvFile) throws IOException {
        BufferedWriter writer = Utility.getWriter(imprvFile);

        for (String qidSid : subtopicImprvs.keySet()) {
            for (Improvement imprv : subtopicImprvs.get(qidSid)) {
                writer.write(qidSid + "-" + imprv.fidx + "-" + imprv.tidx + "\t" + imprv.term);
                for (int i = 0; i < imprv.metricImprvs.length; i++) {
                    writer.write(String.format("\t%.4f", imprv.metricImprvs[i]));
                }
                writer.newLine();
            }
        }
        writer.close();
        Utility.infoWritten(imprvFile);
    }

}