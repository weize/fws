/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintStream;
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
        File outfile = new File(p.getString("facetAnnotationText"));
        BufferedReader reader = Utility.getReader(jsonFile);
        BufferedWriter writer = Utility.getWriter(outfile);
        String line;
        while ((line = reader.readLine()) != null) {
            FeedbackAnnotation feedbackAnnotation = FeedbackAnnotation.parseFromJson(line);
            if (feedbackAnnotation != null) {
                writer.write(feedbackAnnotation.list());
            }
        }
        reader.close();
        writer.close();
        Utility.infoWritten(outfile);
    }

}
