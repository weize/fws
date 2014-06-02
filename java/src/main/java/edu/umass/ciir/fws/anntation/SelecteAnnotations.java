/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.query.QueryTopicSubtopicMap;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class SelecteAnnotations extends AppFunction {

    QueryTopicSubtopicMap queryMap;

    @Override
    public String getName() {
        return "selecte-annotations";
    }

    @Override
    public String getHelpString() {
        return "fws " + getName() + " config.json";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        File selectionFile = new File(p.getString("subtopicSelectedIdFile"));
        queryMap = new QueryTopicSubtopicMap(selectionFile);

        filterFeedbackAnnotation(p);
        filterFacetAnnotation(p);

    }

    private void filterFeedbackAnnotation(Parameters p) throws IOException {
        File jsonFile = new File(p.getString("feedbackAnnotationAllJson"));
        File jsonFilteredFile = new File(p.getString("feedbackAnnotationJson"));
        
        FeedbackAnnotation.select(jsonFile, jsonFilteredFile, queryMap);
        Utility.infoWritten(jsonFilteredFile);
    }
    
     private void filterFacetAnnotation(Parameters p) throws IOException {
        File jsonFile = new File(p.getString("facetAnnotationAllJson"));
        File jsonFilteredFile = new File(p.getString("facetAnnotationJson"));
        
        FacetAnnotation.select(jsonFile, jsonFilteredFile, queryMap);
        Utility.infoWritten(jsonFilteredFile);
    }

}
