/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author wkong
 */
public class TrecEvaluator {

    public String trecEval;
    String metricParam = "-m map -m ndcg_cut.10,20 -m P.10,20,100,1000";
    int metricsNum = 7;
    String results; // stdout returned from trec_eval
    public String[] metrics; // name of metrics read from trec_eval results

    public TrecEvaluator(String trecEval) {
        this.trecEval = trecEval;
    }

    public TrecEvaluator(File resultFile) throws IOException {
        FileInputStream fin = new FileInputStream(resultFile);
        results = Utility.copyStreamToString(fin);
        fin.close();
    }

    public String getHeader() {
        return "#qid\t" + TextProcessing.join(metrics, "\t");
    }

    public void evalAndOutput(String qrelFileName, String rankFileName, File tevalFile) throws Exception {
        evalAndOutput(qrelFileName, rankFileName, tevalFile, null);
    }

    public void evalAndOutput(String qrelFileName, String rankFileName, File tevalfile, File evalFile) throws Exception {
        List<QueryMetrics> qms = eval(qrelFileName, rankFileName);
        Utility.copyStringToFile(results, tevalfile);

        if (evalFile != null) {
            BufferedWriter writer = Utility.getWriter(evalFile);
            //header
            writer.write(getHeader());
            writer.newLine();

            for (QueryMetrics qm : qms) {
                writer.write(qm.toString());
                writer.newLine();
            }
            writer.close();
        }

    }
    
    /**
     * 
     * @param file
     * @param filterAll filter qid "all".
     * @return
     * @throws IOException 
     */
    public static HashMap<String, QueryMetrics> loadQueryMetricsMap(File file, boolean filterAll) throws IOException {
        HashMap<String, QueryMetrics> map = new HashMap<>();
        List<QueryMetrics> list = loadQueryMetricsList(file, filterAll);
        for (QueryMetrics qm : list) {
            map.put(qm.qid, qm);
        }
        return map;
    }

    public static List<QueryMetrics> loadQueryMetricsList(File file, boolean filterAll) throws IOException {
        ArrayList<QueryMetrics> qms = new ArrayList<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("#")) {
                continue;
            }

            String[] fields = line.split("\t");
            String qid = fields[0];
            if (!qid.equals("all")) {
                double[] values = new double[fields.length - 1];
                for (int i = 0; i < values.length; i++) {
                    values[i] = Double.parseDouble(fields[i + 1]);
                }
                QueryMetrics qm = new QueryMetrics(qid, values);
                qms.add(qm);
            }
        }
        reader.close();
        return qms;
    }

    public List<QueryMetrics> eval(String qrelFileName, String rankFileName) throws Exception {
        String cmd = String.format("%s -q %s %s %s", trecEval, metricParam, qrelFileName, rankFileName);
        results = call(cmd);
        return resultToQueryMetrics();

    }

    private String call(String cmd) throws Exception {
        System.err.println(cmd);
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd);

        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = input.readLine()) != null) {
            output.append(line).append("\n");
            //System.err.println(line);
        }
        input.close();

        input = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        while ((line = input.readLine()) != null) {
            System.err.println(line);
        }
        input.close();

        return output.toString();
    }

    public List<QueryMetrics> resultToQueryMetrics() {
        ArrayList<QueryMetrics> qms = new ArrayList<>();
        String[] lines = results.split("\n");
        // read metrics
        metrics = new String[metricsNum];
        for (int j = 0; j < metricsNum; j++) {
            metrics[j] = lines[j].split("\\s+")[0];
        }

        // read for each queries       
        for (int i = 0; i < lines.length; i += metricsNum) {
            // read qid
            String qid = lines[i].split("\\s+")[1];

            // read values for each metric
            String[] values = new String[metricsNum];
            for (int j = 0; j < metricsNum; j++) {
                String[] fields = lines[i + j].split("\\s+");
                String metric = fields[0];
                String curQid = fields[1];
                assert curQid.equals(qid) : "qids not matched: " + lines[i + j];
                assert metric.equals(metrics[j]) : "metrics not matched: " + lines[i + j];
                String value = fields[2];
                values[j] = value;
            }
            qms.add(new QueryMetrics(qid, values));
        }

        return qms;
    }

}
