package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author wkong
 */
public class ExtractFeedbackTimeFromSubtopicActions extends AppFunction {

    BufferedWriter writer;
    List<SubtopicAction> actions;
    HashSet<String> seen; // subtopics visisted for a user;
    boolean hasUnchecked; // if the annotator have performed unchecked action for a subtopic
    SubtopicAction last;
    List<SubtopicAction> session; // term checked;
    boolean started; // started annotations?
    long startTime;

    @Override
    public String getName() {
        return "extract-feedback-time";
    }

    @Override
    public String getHelpString() {
        return "fws extract-feedback-time config.json";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {

        // load actions
        File jsonFile = new File(p.getString("actionJson"));
        actions = SubtopicAction.load(jsonFile);
        Collections.sort(actions, new Comparator<SubtopicAction>() {

            @Override
            public int compare(SubtopicAction o1, SubtopicAction o2) {
                int cmp = o1.aid.compareTo(o2.aid);
                return cmp == 0 ? Long.compare(o1.timeLong, o2.timeLong) : cmp;
            }
        });

        // some initialization
        seen = new HashSet<>();
        session = new ArrayList<>();
        last = null;
        hasUnchecked = false;
        started = false;
        File outfile = new File(p.getString("feedbackTimeFile"));
        writer = Utility.getWriter(outfile);
        for (SubtopicAction action : actions) {
            process(action);
        }
        writer.close();
        Utility.infoWritten(outfile);

    }

    private void process(SubtopicAction action) throws IOException {
        // a new session
        if (last == null
                || !last.aid.equals(action.aid)
                || !last.qid.equals(action.qid)
                || !last.sid.equals(action.sid)) {
            if (last != null) {
                seen.add(last.aid + "\t" + last.qid + "\t" + last.sid);
            }
            init();
        }

        last = action;

        if (started) { // session has been started
            if (action.type.equals("subtopic-check-term")) {
                session.add(action);
            } else if (action.type.equals("subtopic-uncheck-term")) {
                session.add(action);
                hasUnchecked = true;
            } else if (action.type.equals("subtopic-save")) {

            } else if (action.type.equals("subtopic-save-continue")) {
                if (isValidSession()) {
                    emit();
                }
                seen.add(action.aid + "\t" + action.qid + "\t" + action.sid);
                init();
            }
        } else if (action.type.equals("subtopic-show-facet")) {
            started = true;
            startTime = action.timeLong;
        }
    }

//    private void emit() throws IOException {
//        writer.write(last.aid + "\t" + last.qid + "\t" + last.sid + "\t" + startTime + "\t" + hasUnchecked);
//        writer.write("\n-----------------------\n");
//        for (SubtopicAction action : session) {
//            writer.write(action.toString());
//            writer.newLine();
//        }
//        writer.newLine();
//    }
    
    
    private void emit() throws IOException {
        ArrayList<SubtopicAction> part = new ArrayList<>();
        for (SubtopicAction action : session) {
            part.add(action);
        }
        emitSessionPart(part);
    }
    
    
    private void emitSessionPart(List<SubtopicAction> session) throws IOException {
        long numFacetScanned = -1;
        long numTermScanned = 0;
        long lastTime = -1;
        
        HashMap<Long, Long> numTermScannedEachFacet = new HashMap<>();
        for (SubtopicAction action : session) {
            numFacetScanned = (int) Math.max(action.fidx + 1, numFacetScanned);
            long numTermScannedInTheFacet = numTermScannedEachFacet.containsKey(action.fidx) ? numTermScannedEachFacet.get(action.fidx) : 0;
            numTermScannedInTheFacet = Math.max(action.tidx+1, numTermScannedInTheFacet);
            numTermScannedEachFacet.put(action.fidx, numTermScannedInTheFacet);
            lastTime = Math.max(action.timeLong, lastTime);
        }
        
        for (Long fidx : numTermScannedEachFacet.keySet()) {
            numTermScanned += numTermScannedEachFacet.get(fidx);
        }
        
        long time = lastTime - startTime;
        writer.write(String.format("%s\t%s\t%s\t%d\t%d\t%d\n",  last.aid, last.qid, last.sid, numFacetScanned, numTermScanned, time));
        
    }
    

    private boolean isValidSession() {
        return !hasUnchecked
                && !seen.contains(last.aid + "\t" + last.qid + "\t" + last.sid)
                && session.size() > 1;
    }

    private void init() {
        session.clear();
        hasUnchecked = false;
        started = false;
    }

}
