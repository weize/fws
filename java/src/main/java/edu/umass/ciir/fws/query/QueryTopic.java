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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class QueryTopic {

    public String description;
    public String query;
    public String qid;
    public String type;
    public List<QuerySubtopic> subtopics;

    public QueryTopic(String qid, String query, String description, String type) {
        this.qid = qid;
        this.query = query;
        this.description = description;
        this.type = type;
        subtopics = new ArrayList<>();
    }

    public int size() {
        return subtopics.size();
    }

    public void add(QuerySubtopic subtopic) {
        subtopics.add(subtopic);
    }

    public static QueryTopic parseFromJson(String jsonString) throws IOException {
        Parameters data = Parameters.parseString(jsonString);
        String desc = data.getString("description");
        String type = data.getString("type");
        String qid = data.getString("number");
        String query = data.getString("query");

        QueryTopic qt = new QueryTopic(qid, query, desc, type);

        List<Parameters> subtopics = data.getAsList("subtopics", Parameters.class);
        for (Parameters subtopic : subtopics) {
            String sdesc = subtopic.getString("description");
            String stype = subtopic.getString("type");
            String sid = subtopic.getString("number");
            QuerySubtopic qs = new QuerySubtopic(sid, sdesc, stype);
            qt.add(qs);
        }

        return qt;
    }
    
    public static List<QueryTopic> loadQueryFullTopics(File jsonFile) throws IOException {
        ArrayList<QueryTopic> topics = new ArrayList<>();
        BufferedReader reader = Utility.getReader(jsonFile);
        String line;
        while ((line = reader.readLine()) != null) {
            QueryTopic topic = QueryTopic.parseFromJson(line);
            topics.add(topic);
        }
        reader.close();
        return topics;
    }
    
    public static HashMap<String, QueryTopic> loadQueryFullTopicsAsMap(File jsonFile) throws IOException {
        List<QueryTopic> topics = loadQueryFullTopics(jsonFile);
        HashMap<String, QueryTopic> map = new HashMap<>();
        for(QueryTopic t : topics) {
            map.put(t.qid, t);
        }
        return map;
    }
    
    

    public String listAsString() {
        StringBuilder lists = new StringBuilder();
        for (QuerySubtopic s : subtopics) {
            lists.append(String.format("%s\t%s\t%s\t%s\t%s\n", qid, query,
                    description.replaceAll("\\s+", " "), type, s.toString()));
        }
        return lists.toString();
    }

}
