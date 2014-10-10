/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.pub;

import edu.umass.ciir.fws.query.QuerySubtopic;
import edu.umass.ciir.fws.query.QueryTopic;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class GenerateQueryTopicFile extends AppFunction{

    @Override
    public String getName() {
        return "generate-query-topic";
    }

    @Override
    public String getHelpString() {
        return "fws generate-query-topic --output=<output>";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        File queryJsonFile = new File(p.getString("queryJsonFile"));
        File subtopicSelectedIdFile = new File(p.getString("subtopicSelectedIdFile"));
        File outfile  = new File(p.getString("output"));
        
        BufferedReader reader = Utility.getReader(subtopicSelectedIdFile);
        String line;
        HashSet<String> qidSet = new HashSet<>();
        HashSet<String> sidSet = new HashSet<>();
        while((line = reader.readLine())!=null) {
            String [] elems = line.split("-");
            String qid = elems[0];
            String sid = elems[1];
            qidSet.add(qid);
            sidSet.add(line);
        }
        reader.close();
        // load
        List<QueryTopic> all = QueryTopic.loadQueryFullTopics(queryJsonFile);
        List<Parameters> selected = new ArrayList<>();
        for(QueryTopic qt : all) {
            if (qidSet.contains(qt.qid)) {
                //public QueryTopic(String qid, String query, String description, String type) {
              QueryTopic qt2 = new QueryTopic(qt.qid, qt.query, qt.description, qt.type);
              for(QuerySubtopic qs : qt.subtopics) {
                  if (sidSet.contains(qt.qid+"-"+qs.sid)) {
                      qt2.add(qs);
                  }
              }
              selected.add(qt2.toParameters());
            }
        }
        
        
        Parameters data = new Parameters();
        data.put("topics", selected);
        
        BufferedWriter writer = Utility.getWriter(outfile);
        writer.write(data.toPrettyString());
        writer.close();
        Utility.infoWritten(outfile);
    }
}
