/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.FeedbackAnnotation;
import edu.umass.ciir.fws.anntation.FeedbackList;
import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.utility.TextProcessing;
import java.util.ArrayList;

/**
 *
 * @author wkong
 */
public class FacetFeedback {

    public static String FeedbackAnnotationToFfeedbackString(FeedbackAnnotation fa) {
        ArrayList<FeedbackTerm> terms = new ArrayList<>();
        for (FeedbackList list : fa) {
            for (FeedbackTerm term : list) {
                terms.add(term);
            }
        }
        return String.format("%s-%s\t%s\n", fa.qid, fa.sid, TextProcessing.join(terms, "|"));
    }
    
    

}
