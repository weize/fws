/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.experiment;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.ffeedback.FacetFeedback;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class ExtractAnnotatorFeedbackImprovement extends AppFunction {

    @Override
    public String getName() {
        return "extract-annotator-imprv";
    }

    @Override
    public String getHelpString() {
        return "fws extract-annotator-imprv\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        
        File imprvFile = new File("../exp/expansion/oracle/expansion.sts.eval.imprv");
        File outfile = new File("../exp/data/cmp-feedback/annotator.feedback.imprv");
        File annotatorFeedbackFile = new File("../exp/feedback/annotator/annotator.annotator.fdbk");
        

        HashMap<String, FacetFeedback> feedbacks = FacetFeedback.loadGroupByQidSid(annotatorFeedbackFile);
        
        HashMap<String, HashSet<String>> map = new HashMap<>();
        for(String qidSid : feedbacks.keySet()) {
            HashSet<String> set = new HashSet<>();
            for(FeedbackTerm  t : feedbacks.get(qidSid).terms) {
                set.add(t.term);
            }
            map.put(qidSid, set);
        }
        BufferedWriter writer = Utility.getWriter(outfile);
        BufferedReader reader = Utility.getReader(imprvFile);
        String line;
        while((line = reader.readLine())!= null) {
            String [] elems = line.split("\t");
            String [] ids = elems[0].split("-");
            String qidSid = ids[0] + "-"  + ids[1];
            String term = elems[1];
            if (map.get(qidSid).contains(term)) {
                writer.write(line);
                writer.newLine();
                
            }
        }
        reader.close();
        writer.close();
        Utility.infoWritten(outfile);
        

    }
}
