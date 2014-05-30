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
public class Action {

    String annotatorId;
    Date time;
    String type;
    String qid;
    final static long timeDifference = 4 * 60 * 60 * 1000;

    public Action(String annotatorId, String qid, String type, Date time) {
        this.annotatorId = annotatorId;
        this.time = time;
        this.qid = qid;
        this.type = type;
    }

    public static Action parseFromJson(String jsonDataString, boolean filter) throws IOException {
        Parameters data = Parameters.parseString(jsonDataString);
        String aid = data.getString("annotatorID");
        String qid = data.getString("aolUserID");
        Parameters action = data.getMap("action");

        if (filter && aid.startsWith("test-")) {
            return null;
        }
        
        String type = action.getString("type");
        Date time = new Date(action.getLong("time"));
        
        return new Action(aid, qid, type, time);
    }
    
    public static List<Action> load(File jsonFile) throws IOException {
        ArrayList<Action> actions = new ArrayList<>();
        BufferedReader reader = Utility.getReader(jsonFile);
        String line;
        while ((line = reader.readLine()) != null) {
            Action action =Action.parseFromJson(line, true);
            if (action != null) {
                actions.add(action);
            }
        }
        reader.close();
        return actions;
    }
    
    @Override
    public String toString() {
        return String.format("%s\t%d\t%s\t%s\t%s", annotatorId, time.getTime(), time.toLocaleString(), type, qid);
    }
}
