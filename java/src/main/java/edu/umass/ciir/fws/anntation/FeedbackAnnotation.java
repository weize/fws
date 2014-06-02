/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.emory.mathcs.backport.java.util.Collections;
import static edu.umass.ciir.fws.anntation.FacetAnnotation.load;
import edu.umass.ciir.fws.query.QueryTopicSubtopicMap;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Facet feedbacks from annotators for a subtopic.
 *
 * @author wkong
 */
public class FeedbackAnnotation implements Iterable<FeedbackList> {

    public String aid; // annotator id
    public String qid; // query id
    public String sid; //subtopic id
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

    public void sortFeedbackFacet() {
        Collections.sort(feedbacks);
    }

    public static FeedbackAnnotation parseFromJson(String jsonDataString) throws IOException {
        return parseFromJson(jsonDataString, true);
    }

    public static FeedbackAnnotation parseFromJson(String jsonDataString, boolean filter) throws IOException {
        Parameters data = Parameters.parseString(jsonDataString);
        String aid = data.getString("annotatorID");
        String qid = data.getString("aolUserID");
        //String sid = data.getString("subtopicID");
        String sid = String.valueOf(Integer.parseInt(data.getString("subtopicID")) + 1); // change from zero-based to one-based
        Parameters feedbackMap = data.getMap("explicitlySaved");

        FeedbackAnnotation fa = new FeedbackAnnotation(aid, qid, sid);
        if (filter && aid.startsWith("test-")) {
            return null;
        }

        for (String fid : feedbackMap.getKeys()) {
            Parameters feedback = feedbackMap.getMap(fid);
            int fidx = (int) feedback.getLong("index");
            Parameters terms = feedback.getMap("terms");

            FeedbackList list = new FeedbackList(fid, fidx);
            for (String tid : terms.keySet()) {
                Parameters term = terms.getMap(tid);
                FeedbackTerm ft = new FeedbackTerm(term.getString("term"), fidx, (int) term.getLong("index"));
                list.add(ft);
            }
            if (list.size() > 0) {
                list.sortTerms();
                fa.add(list);
            }
        }
        fa.sortFeedbackFacet();
        return fa;
    }

    public static List<FeedbackAnnotation> load(File jsonFile) throws IOException {
        ArrayList<FeedbackAnnotation> annotations = new ArrayList<>();
        BufferedReader reader = Utility.getReader(jsonFile);
        String line;
        while ((line = reader.readLine()) != null) {
            FeedbackAnnotation annotation = FeedbackAnnotation.parseFromJson(line);
            if (annotation != null) {
                annotations.add(annotation);
            }
        }
        reader.close();
        return annotations;
    }

    public static HashMap<String, FeedbackAnnotation> loadAsMap(File jsonFile) throws IOException {
        HashMap<String, FeedbackAnnotation> map = new HashMap<>();
        List<FeedbackAnnotation> annotation = load(jsonFile);
        for (FeedbackAnnotation a : annotation) {
            map.put(a.qid, a);
        }
        return map;
    }

    public String list() {
        StringBuilder lists = new StringBuilder();
        for (FeedbackList list : feedbacks) {
            lists.append(String.format("%s\t%s\t%s\t%s\n", aid, qid, sid, list.toString()));
        }
        return lists.toString();
    }

    @Override
    public Iterator<FeedbackList> iterator() {
        return feedbacks.iterator();
    }

    public static void select(File jsonFile, File jsonFilteredFile, QueryTopicSubtopicMap queryMap) throws IOException {
        BufferedReader reader = Utility.getReader(jsonFile);
        BufferedWriter writer = Utility.getWriter(jsonFilteredFile);
        String line;
        while ((line = reader.readLine()) != null) {
            Parameters p = filterJson(line, queryMap);
            if (p!= null) {
                writer.write(p.toString());
                writer.newLine();
            }
        }
        reader.close();
        writer.close();

    }

    public static Parameters filterJson(String jsonDataString, QueryTopicSubtopicMap queryMap) throws IOException {
        Parameters data = Parameters.parseString(jsonDataString);
        String aid = data.getString("annotatorID");
        String qid = data.getString("aolUserID");
        //String sid = data.getString("subtopicID");
        String sid = String.valueOf(Integer.parseInt(data.getString("subtopicID")) + 1); // change from zero-based to one-based

        if (queryMap.hasTopic(qid) && queryMap.hasSubtopic(qid, sid) && !aid.startsWith("test-")) {
            return data;
        } else {
            return null;
        }
    }

}
