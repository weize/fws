/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.anntation;

import edu.emory.mathcs.backport.java.util.Collections;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Facet feedbacks from annotators for a subtopic.
 * @author wkong
 */
public class FeedbackAnnotation {
    String aid; // annotator id
    String qid; // query id
    String sid; //subtopic id
    ArrayList<FeedbackList> feedbacks;
    
    public FeedbackAnnotation(String aid, String qid, String sid) {
        this.aid = aid;
        this.qid = qid;
        this.sid = sid;
        this.feedbacks = new ArrayList<>();
    }
    
    public void add(FeedbackList list) {
        feedbacks.add(list);
    }
    
    
    public static FeedbackAnnotation parseFromJson(String jsonDataString) throws IOException {
        return parseFromJson(jsonDataString, true);
    }
    
    public static FeedbackAnnotation parseFromJson(String jsonDataString, boolean filter) throws IOException {
        Parameters data = Parameters.parseString(jsonDataString);
        String aid = data.getString("annotatorID");
        String qid = data.getString("aolUserID");
        //String sid = data.getString("subtopicID");
        String sid = String.valueOf(Integer.parseInt(data.getString("subtopicID") + 1)); // change from zero-based to one-based
        Parameters feedbackMap = data.getMap("explicitlySaved");
        
        FeedbackAnnotation fa = new FeedbackAnnotation(aid, qid, sid);
        if (filter && aid.startsWith("test-")) {
            return null;
        }
        
        // to sort ids
        ArrayList<Integer> facetIds = new ArrayList<>();
        for (String fid : feedbackMap.getKeys()) {
            facetIds.add(Integer.parseInt(fid));
        }
        Collections.sort(facetIds);
        
        for (Integer fid : facetIds) {
            Parameters feedback = feedbackMap.getMap(fid.toString());
            int index = (int)feedback.getLong("index");
            Parameters terms = feedback.getMap("terms");
            
            FeedbackList list = new FeedbackList(fid, index);
            for(String tid : terms.keySet()) {
                Parameters term = terms.getMap(tid);
                FeedbackTerm ft = new FeedbackTerm(term.getString("term"), (int)term.getLong("index"));
                list.add(ft);
            }
            if (list.size()>0) {
                fa.add(list);
            }
        }
        
        return fa;
    }

    public String list() {
        StringBuilder lists = new StringBuilder();
        for(FeedbackList list : feedbacks) {
            lists.append(String.format("%s\t%s\t%s\t%s\n", aid, qid, sid, list.toString()));
        }
        return lists.toString();
    }
    
}
