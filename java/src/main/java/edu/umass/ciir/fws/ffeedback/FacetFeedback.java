/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.anntation.FeedbackAnnotation;
import edu.umass.ciir.fws.anntation.FeedbackList;
import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.utility.TextProcessing;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author wkong
 */
public class FacetFeedback {

    String qid;
    String sid;
    public List<List<FeedbackTerm>> facets;
    public List<FeedbackTerm> terms;

    public static String FeedbackAnnotationToFfeedbackString(FeedbackAnnotation fa) {
        ArrayList<FeedbackTerm> terms = new ArrayList<>();
        for (FeedbackList list : fa) {
            for (FeedbackTerm term : list) {
                terms.add(term);
            }
        }
        return String.format("%s-%s\t%s\n", fa.qid, fa.sid, TextProcessing.join(terms, "|"));
    }

    public String termsToString() {
        return TextProcessing.join(terms, "|");
    }

    public static FacetFeedback parseFromString(String line) {
        String[] elems = line.split("\t");

	FacetFeedback ff;
	if (elems.length == 2) {
        	ff = parseTerms(elems[1]);
	} else {
		ff = new FacetFeedback();
		ff.terms = new ArrayList<>();
		ff.facets = new ArrayList<>();
	}
	ff.qid = elems[0].split("-")[0];
	ff.sid = elems[0].split("-")[1];
        return ff;
    }

    public static FacetFeedback parseTerms(String termsStr) {
        FacetFeedback ff = new FacetFeedback();
        ff.terms = new ArrayList<>();
        ff.facets = new ArrayList<>();
	if(termsStr.isEmpty()) {
            return ff;
	}
        // set terms
        for (String termString : termsStr.split("\\|")) {
            FeedbackTerm ft = FeedbackTerm.parseFromString(termString);
            ff.terms.add(ft);
        }
        Collections.sort(ff.terms);

        // group terms
        int fidx = -1;
        ArrayList<FeedbackTerm> list = null;
        for (FeedbackTerm ft : ff.terms) {
            if (list == null) {
                list = new ArrayList<>();
                fidx = ft.fidx;
            } else if (fidx != ft.fidx) {
                ff.facets.add(list);
                list = new ArrayList<>();
                fidx = ft.fidx;
            }

            list.add(ft);
        }

        if (list != null) {
            ff.facets.add(list);
        }
        return ff;
    }

    public static String toUniqueExpansionString(List<FeedbackTerm> terms) {
        Collections.sort(terms);
        return TextProcessing.join(terms, "|").replace('-', '~');
    }

    public static FacetFeedback parseTermsFromUniqueExpansionString(String uniqueExpansionString) {
        return parseTerms(uniqueExpansionString.replace('~', '-'));
    }

}
