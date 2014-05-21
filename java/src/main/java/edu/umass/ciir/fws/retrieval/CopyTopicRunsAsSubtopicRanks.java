/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.retrieval;

import edu.umass.ciir.fws.crawl.QuerySetResults;
import edu.umass.ciir.fws.query.QuerySubtopic;
import edu.umass.ciir.fws.query.QueryTopic;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * Copy ranks for each query as ranks for all its subtopic queries.
 *
 * @author wkong
 */
public class CopyTopicRunsAsSubtopicRanks extends AppFunction{

    @Override
    public String getName() {
        return "copy-as-subtopic-rank";
    }

    @Override
    public String getHelpString() {
        return "fws copy-as-subtopic-rank config.json";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        // load rank results file
        String rankedListFile = p.getString("rankedListFile");
        File queryJsonFile = new File(p.getString("queryJsonFile"));
        File outfile  = new File(p.getString("subtopicRankFile"));
        
        // load
        QuerySetResults ranks = new QuerySetResults(rankedListFile, 1000);
        HashMap<String, QueryTopic> topics = QueryTopic.loadQueryFullTopicsAsMap(queryJsonFile);
        
        BufferedWriter writer = Utility.getWriter(outfile);
        for (String qid : ranks.getQueryIterator()) {
            QueryTopic topic = topics.get(qid);
            for (QuerySubtopic subtopic : topic.subtopics) {
                for (ScoredDocument sd : ranks.get(qid).getIterator()) {
                    String newQid = qid + "-" + subtopic.sid;
                    writer.write(sd.toTRECformat(newQid));
                    writer.newLine();
                }
            }
        }
        writer.close();
        Utility.infoWritten(outfile);
    }

}
