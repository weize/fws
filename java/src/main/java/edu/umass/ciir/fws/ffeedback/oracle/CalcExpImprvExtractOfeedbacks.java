/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback.oracle;

import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
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
public class CalcExpImprvExtractOfeedbacks extends AppFunction {

    @Override
    public String getName() {
        return "extract-oracle-feedbacks";
    }

    @Override
    public String getHelpString() {
        return "fws " + getName() + " config.json";
    }

    static class Improvement {

        String termId;
        String term;
        double[] metricImprvs;

        public Improvement(String termId, String term, double[] metricImprvs) {
            this.termId = termId;
            this.term = term;
            this.metricImprvs = metricImprvs;
        }

    }

    private TreeMap<String, ArrayList<Improvement>> calcImprovements(File oracleExpansionEvalFile, File sdmSevalFile, File expandedTermFile) throws IOException {
        HashMap<String, String> expTermMap = loadExpTermMap(expandedTermFile);
        HashMap<String, QueryMetrics> sdmQms = TrecEvaluator.loadQueryMetricsMap(sdmSevalFile, true);
        List<QueryMetrics> expQms = TrecEvaluator.loadQueryMetricsList(oracleExpansionEvalFile, true);

        TreeMap<String, ArrayList<Improvement>> subtopicImprvs = new TreeMap<>();
        // qid-sid  
        for (QueryMetrics qm : expQms) {
            double[] improvement = new double[qm.values.length];
            String[] qidSidTermId = qm.qid.split("-");
            String qid = qidSidTermId[0];
            String sid = qidSidTermId[1];
            String termId = qidSidTermId[2];

            String qidSid = qid + "-" + sid;
            QueryMetrics sdmQm = sdmQms.get(qidSid);
            for (int i = 0; i < improvement.length; i++) {
                improvement[i] = qm.values[i] - sdmQm.values[i];
            }

            String term = expTermMap.get(qid + "-" + termId);
            Improvement imprv = new Improvement(termId, term, improvement);

            if (subtopicImprvs.containsKey(qidSid)) {
                subtopicImprvs.get(qidSid).add(imprv);
            } else {
                ArrayList<Improvement> list = new ArrayList<>();
                list.add(imprv);
                subtopicImprvs.put(qidSid, list);
            }

        }
        return subtopicImprvs;
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {

        File expandedTermFile = new File(p.getString("oracleExpandedTerms"));
        File sdmSevalFile = new File(p.getString("sdmSeval"));
        File oracleExpansionEvalFile = new File(p.getString("oracleExpansionEvalFile"));
        File imprvFile = new File(p.getString("oracleExpansionEvalImproveFile"));
        String feedbackDir = p.getString("oracleFeedbackDir");
        List<Double> threshoulds = p.getAsList("oracleFeedbackImprvThresholds", Double.class);

        TreeMap<String, ArrayList<Improvement>> subtopicImprvs = calcImprovements(
                oracleExpansionEvalFile, sdmSevalFile, expandedTermFile);

        outputImprovementFile(subtopicImprvs, imprvFile);

        for (double threshold : threshoulds) {
            outputFeedbackTerms(subtopicImprvs, threshold, feedbackDir);
        }

    }

    private void outputFeedbackTerms(TreeMap<String, ArrayList<Improvement>> subtopicImprvs, double threshold, String feedbackDir) throws IOException {
        File outfile = new File(Utility.getOracleFeedbackFile(feedbackDir, threshold));
        BufferedWriter writer = Utility.getWriter(outfile);
         for (String qidSid : subtopicImprvs.keySet()) {
            ArrayList<Improvement> selected = new ArrayList<>();
            for (Improvement imprv : subtopicImprvs.get(qidSid)) {
                if (imprv.metricImprvs[0] >= threshold) {
                    selected.add(imprv);
                }
            }
            String [] terms = new String[selected.size()];
            for (int i = 0; i < terms.length; i ++) {
                terms[i] = selected.get(i).term;
            }
            writer.write(String.format("%s\t%s\n", qidSid,TextProcessing.join(terms, "|")));
        }
        writer.close();
        Utility.infoWritten(outfile);
    }

    private HashMap<String, String> loadExpTermMap(File expandedTermFile) throws IOException {
        BufferedReader reader = Utility.getReader(expandedTermFile);

        HashMap<String, String> map = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            String qid = fields[0];
            String termId = fields[1];
            String fidTid = fields[2];
            String query = fields[3];
            String term = fields[4];
            map.put(qid + "-" + termId, term);
        }
        reader.close();
        return map;
    }

    private void outputImprovementFile(TreeMap<String, ArrayList<Improvement>> subtopicImprvs, File imprvFile) throws IOException {
        BufferedWriter writer = Utility.getWriter(imprvFile);

        for (String qidSid : subtopicImprvs.keySet()) {
            for (Improvement imprv : subtopicImprvs.get(qidSid)) {
                writer.write(qidSid + "-" + imprv.termId + "\t" + imprv.term);
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
