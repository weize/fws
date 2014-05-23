/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ofeedback;

import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class CalcFacetTermExpImprovement extends AppFunction {
    
    @Override
    public String getName() {
        return "calc-facet-term-improvement";
    }
    
    @Override
    public String getHelpString() {
        return "fws " + getName() + " config.json";
    }
    
    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        File expandedTermFile = new File(p.getString("oracleExpandedTerms"));
        File sdmSevalFile = new File(p.getString("sdmSeval"));
        File oracleExpansionEvalFile = new File(p.getString("oracleExpansionEvalFile"));
        File outfile = new File(p.getString("oracleExpansionEvalImproveFile"));
        HashMap<String, String> expTermMap = loadExpTermMap(expandedTermFile);
        HashMap<String, QueryMetrics> sdmQms = TrecEvaluator.loadQueryMetricsMap(sdmSevalFile, true);
        
        List<QueryMetrics> expQms = TrecEvaluator.loadQueryMetricsList(oracleExpansionEvalFile, true);
        BufferedWriter writer = Utility.getWriter(outfile);
        for (QueryMetrics qm : expQms) {
            double[] improvement = new double[qm.values.length];
            String[] qidSidFidTid = qm.qid.split("-");
            String qid = qidSidFidTid[0];
            String sid = qidSidFidTid[1];
            String fid = qidSidFidTid[2];
            String tid = qidSidFidTid[3];
            
            QueryMetrics sdmQm = sdmQms.get(qid+"-"+sid);
            for (int i = 0; i < improvement.length; i++) {
                improvement[i] = qm.values[i] - sdmQm.values[i];
            }
            
            String term = expTermMap.get(qid + "-" + fid + "-" + tid);
            
            writer.write(qm.qid + "\t" + term);
            for (int i = 0; i < improvement.length; i++) {
                writer.write(String.format("\t%.4f",improvement[i]));
            }
            writer.newLine();
            
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
    
}
