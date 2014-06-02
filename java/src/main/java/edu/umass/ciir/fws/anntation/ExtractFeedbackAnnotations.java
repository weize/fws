/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.ffeedback.FacetFeedback;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class ExtractFeedbackAnnotations extends AppFunction {

    @Override
    public String getName() {
        return "extract-feedback-annotations";
    }

    @Override
    public String getHelpString() {
        return "fws extract-feedback-annotations config.json";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {

        File jsonFile = new File(p.getString("feedbackAnnotationJson"));
        List<FeedbackAnnotation> annotations = FeedbackAnnotation.load(jsonFile);

        File annotatoTextFile = new File(p.getString("feedbackAnnotationText"));
        extractFeedbackAnnotationText(annotations, annotatoTextFile);
        Utility.infoWritten(annotatoTextFile);

        File annotatorFeedbackFile = new File(p.getString("annotatorFeedback"));
        extractAnnotatorFeedback(annotations, annotatorFeedbackFile);
        Utility.infoWritten(annotatorFeedbackFile);
    }

    private void extractFeedbackAnnotationText(List<FeedbackAnnotation> annotations, File outfile) throws IOException {
        BufferedWriter writer = Utility.getWriter(outfile);

        writer.write("#anntatorID\tqid\tsid\tfid\tfidx\tterms(<fid-tid:term>|...)\n");
        for (FeedbackAnnotation feedbackAnnotation : annotations) {
            writer.write(feedbackAnnotation.list());
        }
        writer.close();

    }

    private void extractAnnotatorFeedback(List<FeedbackAnnotation> annotations, File outfile) throws IOException {
        BufferedWriter writer = Utility.getWriter(outfile);
        for (FeedbackAnnotation an : annotations) {            
                writer.write(FacetFeedback.FeedbackAnnotationToFfeedbackString(an));
        }
        writer.close();
    }

}
