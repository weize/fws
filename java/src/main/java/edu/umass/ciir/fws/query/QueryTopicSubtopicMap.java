/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.query;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/**
 *
 * @author wkong
 */
public class QueryTopicSubtopicMap {
    HashSet<String> topics;
    HashSet<String> subtopics;
    
    public QueryTopicSubtopicMap(File file) throws IOException {
        loadTopicSubtopics(file);
    }
    
    private void loadTopicSubtopics(File selectionFile) throws IOException {
        topics = new HashSet<>();
        subtopics = new HashSet<>();
        
        BufferedReader in = Utility.getReader(selectionFile);
        String line;
        while((line = in.readLine())!= null) {
            String [] elems = line.split("-");
            String qid = elems[0];
            String sid = elems[1];
            topics.add(qid);
            subtopics.add(line);
        }
        in.close();
        
    }

    public boolean hasTopic(String qid) {
        return topics.contains(qid);
    }

    public boolean hasSubtopic(String qid, String sid) {
        return subtopics.contains(qid+"-"+sid);
    }
    
    
    
    
}
