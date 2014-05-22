/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.subtopic;

import edu.umass.ciir.fws.query.QuerySubtopic;
import edu.umass.ciir.fws.query.QueryTopic;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import static edu.umass.ciir.fws.utility.Utility.infoWritten;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class Fix09DiversityQrel extends AppFunction {

    @Override
    public String getName() {
        return "fix-09-diversity-qrel";
    }

    @Override
    public String getHelpString() {
        return "fws fix-09-diversity-qrel config.json";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        // load rank results file
        count(p);
        //fix09DiversityQrel(p);
    }

    private void count(Parameters p) throws IOException {
        File queryJsonFile = new File(p.getString("queryJsonFile"));
        File inputfile = new File("../data/qrel/num-rel.subtopic.count");
        File outfile = new File("../data/qrel/num-rel.subtopic.2.count");

        HashMap<String, QueryTopic> topics = QueryTopic.loadQueryFullTopicsAsMap(queryJsonFile);

        HashMap<String, Integer> counts = new HashMap<>();
        BufferedReader reader = Utility.getReader(inputfile);
        BufferedWriter writer = Utility.getWriter(outfile);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] elms = line.split("\\s+");
            String qid = elms[0];
            String sid = elms[1];
            int count = Integer.parseInt(elms[2]);
            counts.put(qid + "-" + sid, count);
        }

        for (String qid : topics.keySet()) {
            for (QuerySubtopic s : topics.get(qid).subtopics) {
                String qidSid = qid + "-" + s.sid;
                int count = counts.containsKey(qidSid) ? counts.get(qidSid) : 0;
                writer.write(String.format("%s\t%s\t%d\n", qid, s.sid, count));
            }
        }
        writer.close();
        reader.close();
        Utility.infoWritten(outfile);
    }

    private void fix09DiversityQrel(Parameters p) throws IOException {

        File queryJsonFile = new File(p.getString("queryJsonFile"));
        File inputfile = new File("09.diversity-need-fix.sqrel");
        File outfile = new File("09.diversity.sqrel");

        HashMap<String, QueryTopic> topics = QueryTopic.loadQueryFullTopicsAsMap(queryJsonFile);

        BufferedReader reader = Utility.getReader(inputfile);
        BufferedWriter writer = Utility.getWriter(outfile);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] elms = line.split("\\s+");
            String qid = elms[0];
            String sid = elms[1];
            if (sid.equals("0")) {
                for (QuerySubtopic subtopic : topics.get(qid).subtopics) {
                    elms[1] = subtopic.sid;
                    writer.write(TextProcessing.join(elms, " "));
                    writer.newLine();
                }
            } else {
                writer.write(line);
                writer.newLine();
            }

        }
        writer.close();
        reader.close();
        Utility.infoWritten(outfile);
    }

}
