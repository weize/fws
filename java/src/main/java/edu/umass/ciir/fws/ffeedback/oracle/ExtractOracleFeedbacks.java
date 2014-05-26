/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback.oracle;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
import edu.umass.ciir.fws.ffeedback.FacetFeedback;
import edu.umass.ciir.fws.ffeedback.QuerySubtopicExpansion;
import static edu.umass.ciir.fws.ffeedback.RunExpasions.setParameters;
import edu.umass.ciir.fws.utility.Utility;
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

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        p.set("expansionSource", "allterm");
        p.set("expansionModel", "sts");
        setParameters(p);
        
        File expansionFile = new File(p.getString("expansionFile"));
        String model = p.getString("expansionModel");
        File sdmSevalFile = new File(p.getString("sdmSeval"));
        File expansionEvalFile = new File(p.getString("expansionEvalFile"));
        String expansionEvalImprFile = expansionEvalFile + ".imprv";
        String feedbackDir = p.getString("oracleFeedbackDir");
        List<Double> threshoulds = p.getAsList("oracleFeedbackImprvThresholds", Double.class);

        
        TreeMap<String, ArrayList<Improvement>> subtopicImprvs = calcImprovements(
                expansionEvalFile, sdmSevalFile, expansionFile, model);

        outputImprovementFile(subtopicImprvs, expansionEvalImprFile, model);

        for (double threshold : threshoulds) {
            outputFeedbackTerms(subtopicImprvs, threshold, feedbackDir, model);
        }

    }

    private TreeMap<String, ArrayList<Improvement>> calcImprovements(File expansionEvalFile, File sdmSevalFile, File expansionFile, String model) throws IOException {
        Utility.infoProcessing(expansionFile);
        Utility.infoProcessing(sdmSevalFile);
        Utility.infoProcessing(expansionEvalFile);
        HashMap<String, QuerySubtopicExpansion> qseMap = QuerySubtopicExpansion.loadQueryExpansionAsMap(expansionFile, model);
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

                QuerySubtopicExpansion qes = qseMap.get(QuerySubtopicExpansion.toId(qid, sid, model, expId));
                FeedbackTerm term = FeedbackTerm.parseFromString(qes.expansion);
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

    private void outputFeedbackTerms(TreeMap<String, ArrayList<Improvement>> subtopicImprvs, double threshold, String feedbackDir, String model) throws IOException {
        File outfile = new File(Utility.getOracleFeedbackFile(feedbackDir, model, threshold));
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

    private void outputImprovementFile(TreeMap<String, ArrayList<Improvement>> subtopicImprvs, String expansionEvalImprFile, String model) throws IOException {
        File outfile = new File(expansionEvalImprFile);
        BufferedWriter writer = Utility.getWriter(outfile);

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
        Utility.infoWritten(outfile);
    }

}
