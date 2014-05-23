/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ofeedback;

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
public class CalcFacetTermExpImprovement extends AppFunction {

    @Override
    public String getName() {
        return "extract-oracle-feedbacks";
    }

    @Override
    public String getHelpString() {
        return "fws " + getName() + " config.json";
    }

    static class Improvement {

        String fid, tid;
        String term;
        double[] metricImprvs;

        public Improvement(String fid, String tid, String term, double[] metricImprvs) {
            this.fid = fid;
            this.tid = tid;
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
            String[] qidSidFidTid = qm.qid.split("-");
            String qid = qidSidFidTid[0];
            String sid = qidSidFidTid[1];
            String fid = qidSidFidTid[2];
            String tid = qidSidFidTid[3];

            String qidSid = qid + "-" + sid;
            QueryMetrics sdmQm = sdmQms.get(qidSid);
            for (int i = 0; i < improvement.length; i++) {
                improvement[i] = qm.values[i] - sdmQm.values[i];
            }

            String term = expTermMap.get(qid + "-" + fid + "-" + tid);
            Improvement imprv = new Improvement(fid, tid, term, improvement);

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
            String query = fields[1];
            String fidTid = fields[2];
            String term = fields[3];
            map.put(qid + "-" + fidTid, term);
        }
        reader.close();
        return map;
    }

    private void outputImprovementFile(TreeMap<String, ArrayList<Improvement>> subtopicImprvs, File imprvFile) throws IOException {
        BufferedWriter writer = Utility.getWriter(imprvFile);

        for (String qidSid : subtopicImprvs.keySet()) {
            for (Improvement imprv : subtopicImprvs.get(qidSid)) {
                writer.write(qidSid + "-" + imprv.fid + "-" + imprv.tid + "\t" + imprv.term);
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
