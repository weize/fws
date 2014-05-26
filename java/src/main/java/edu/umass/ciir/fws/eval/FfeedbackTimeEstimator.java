/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.ffeedback.FacetFeedback;
import java.util.List;

/**
 *
 * @author wkong
 */
public class FfeedbackTimeEstimator {
    final static int ftRatio = 2; 

    public static int time(FacetFeedback ffeedback) {
        // fidx: index of the facet. this is zero-based
        int numFacetScanned = ffeedback.terms.get(ffeedback.terms.size() - 1).fidx + 1;
        int numTermScanned = 0;
        for (List<FeedbackTerm> facet : ffeedback.facets) {
            numTermScanned += facet.get(facet.size()-1).tidx + 1;
        }
        int time = numFacetScanned * ftRatio + numTermScanned;
        return time;
    }

}
