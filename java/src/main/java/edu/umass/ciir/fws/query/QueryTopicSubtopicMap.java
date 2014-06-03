/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.query;

import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author wkong
 */
public class QueryTopicSubtopicMap {
    HashMap<String, List<String>> topics; // qid->[sid]
    HashSet<String> subtopics; // sid
    HashMap<String, TfQuery> queries; // qid -> query
    
    public QueryTopicSubtopicMap(File file) throws IOException {
        loadTopicSubtopics(file);
    }

    public QueryTopicSubtopicMap(File selectionFile, File queryFile) throws IOException {
        loadTopicSubtopics(selectionFile);
        queries = QueryFileParser.loadQueryMap(queryFile);
    }
    
    private void loadTopicSubtopics(File selectionFile) throws IOException {
        topics = new HashMap<>();
        subtopics = new HashSet<>();
        
        BufferedReader in = Utility.getReader(selectionFile);
        String line;
        while((line = in.readLine())!= null) {
            String [] elems = line.split("-");
            String qid = elems[0];
            String sid = elems[1];
            if (!topics.containsKey(qid)) {
                topics.put(qid, new ArrayList<String>());
            }
            topics.get(qid).add(sid);
            subtopics.add(line);
        }
        in.close();
        
    }

    public boolean hasTopic(String qid) {
        return topics.keySet().contains(qid);
    }

    public boolean hasSubtopic(String qid, String sid) {
        return subtopics.contains(qid+"-"+sid);
    }
    
    public Set<String> getQidSet() {
        return topics.keySet();
    }

    public List<String> getSidSet(String qid) {
        return topics.get(qid);
    }

    public String getQuery(String qid) {
        return queries.get(qid).text;
    }
    
    
    
    
}
