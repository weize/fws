/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class SubtopicAction {

    String aid;
    String qid;
    String sid;
    long timeLong;
    Date time;
    String type;
    long fidx;
    long tidx;
    //final static long timeDifference = 4 * 60 * 60 * 1000;

    public SubtopicAction(String annotatorId, String qid, String sid, String type, Date time, long fidx, long tidx) {
        this.aid = annotatorId;
        this.time = time;
        this.timeLong = time.getTime();
        this.qid = qid;
        this.sid = sid;
        this.type = type;
        this.fidx = fidx;
        this.tidx = tidx;
    }

    public static SubtopicAction parseFromJson(String jsonDataString, boolean filter) throws IOException {
        Parameters data = Parameters.parseString(jsonDataString);
        String aid = data.getString("annotatorID");
        String qid = data.getString("aolUserID");

        Parameters action = data.getMap("action");

        if (filter && aid.startsWith("test-")) {
            return null;
        }
        
        // andrew1 -> andrew
        aid = aid.replaceAll("\\d+$", "");

        String type = action.getString("type");

        if (type.startsWith("subtopic")) {
            String sid = data.getString("subtopicID");
            Date time = new Date(action.getLong("time"));
            long fidx = -1;
            long tidx = -1;
            if (type.endsWith("-term")) {
                fidx = action.getLong("facetIndex");
                tidx = action.getLong("termIndex");
            }
            return new SubtopicAction(aid, qid, sid, type, time, fidx, tidx);
        } else {
            return null;
        }
    }

    public static List<SubtopicAction> load(File jsonFile) throws IOException {
        ArrayList<SubtopicAction> actions = new ArrayList<>();
        BufferedReader reader = Utility.getReader(jsonFile);
        String line;
        while ((line = reader.readLine()) != null) {
            SubtopicAction action = parseFromJson(line, true);
            if (action != null) {
                actions.add(action);
            }
        }
        reader.close();
        return actions;
    }
    
    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%s\t%d\t%s\t%d\t%d", aid, qid, sid, type, time.getTime(), time.toLocaleString(), fidx, tidx);
    }
 }
