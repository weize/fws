/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.study;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.ffeedback.FacetFeedback;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class CmpAnnotatorOracleFeedback extends AppFunction {

    @Override
    public String getName() {
        return "cmp-feedbacks";
    }

    @Override
    public String getHelpString() {
        return "fws cmp-feedbacks\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        File oracleFile = new File("../exp/feedback/annotator/annotator.oracle.stb-0_01.fdbk");
        File annotatorFile = new File("../exp/feedback/annotator/annotator.annotator.fdbk");
        File annotatorStatFile = new File("../exp/data/cmp-feedback/feedback.annotator.stats");
        File oracleStatFile = new File("../exp/data/cmp-feedback/feedback.oracle.stats");
        File evalFile = new File("../exp/data/cmp-feedback/feedback.cmp.eval");

        HashMap<String, FacetFeedback> oracle = FacetFeedback.loadGroupByQidSid(oracleFile);
        HashMap<String, FacetFeedback> annotator = FacetFeedback.loadGroupByQidSid(annotatorFile);

        outputStats(annotator, annotatorStatFile);
        outputStats(oracle, oracleStatFile);
        outputEval(annotator, oracle, evalFile);

    }

    private void outputStats(HashMap<String, FacetFeedback> feedbacks, File out) throws IOException {
        BufferedWriter writer = Utility.getWriter(out);
        double termAvg =0, facetAvg =0;
        double hasFeedback = 0;
        for (String qidSid : feedbacks.keySet()) {
            FacetFeedback feedback = feedbacks.get(qidSid);
            writer.write(String.format("%s\t%d\t%d\n", qidSid, feedback.terms.size(), feedback.facets.size()));
            termAvg +=feedback.terms.size();
            facetAvg += feedback.facets.size();
            if (feedback.terms.size()>0) {
                hasFeedback += 1;
            }
        }
        int size = feedbacks.keySet().size();
        termAvg /= size;
        facetAvg /= size;
        hasFeedback /= size;
        writer.write(String.format("%s\t%.4f\t%.4f\t%.4f\n", "all", termAvg, facetAvg, hasFeedback));
        writer.close();
        Utility.infoWritten(out);
    }

    private void outputEval(HashMap<String, FacetFeedback> annotator, HashMap<String, FacetFeedback> oracle, File out) throws IOException {
        BufferedWriter writer = Utility.getWriter(out);

        double pAvg = 0, rAvg = 0, f1Avg = 0;
        for (String qidSid : annotator.keySet()) {
            FacetFeedback an = annotator.get(qidSid);
            FacetFeedback or = oracle.get(qidSid);

            HashSet<String> anSet = toHashSet(an);
            HashSet<String> orSet = toHashSet(or);

            int correct = 0;
            for (String t : anSet) {
                if (orSet.contains(t)) {
                    correct++;
                }
            }

            int anSize = anSet.size();
            int orSize = orSet.size();
            double p, r;

            if (orSize == 0) {
                p = anSize == 0 ? 1 : 0;
                r = 1;
            } else {
                p = anSize == 0 ? 0 : (double) correct / (double) anSize;
                r = (double) correct / (double) orSize;
            }

            double f1 = (p + r) < Utility.epsilon ? 0 : 2 * p * r / (p + r);

            pAvg += p;
            rAvg += r;
            f1Avg += f1;

            writer.write(String.format("%s\t%.4f\t%.4f\t%.4f\n", qidSid, p, r, f1));
        }

        int size = annotator.keySet().size();
        pAvg /= size;
        rAvg /= size;
        f1Avg /= size;

        writer.write(String.format("%s\t%.4f\t%.4f\t%.4f\n", "all", pAvg, rAvg, f1Avg));
        writer.close();
        Utility.infoWritten(out);
    }

    private HashSet<String> toHashSet(FacetFeedback feedback) {
        HashSet<String> set = new HashSet<>();
        for (FeedbackTerm t : feedback.terms) {
            set.add(t.term);
        }
        return set;
    }

}
