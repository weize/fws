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
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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

    public FacetFeedback(String qidSid, ArrayList<FeedbackTerm> terms) {
        this.qid = qidSid.split("-")[0];
        this.sid = qidSid.split("-")[1];
        Collections.sort(terms);
        this.terms = terms;
    }

    public FacetFeedback() {

    }

    public String termsToString() {
        return TextProcessing.join(terms, "|");
    }

    public static FacetFeedback parseFromStringAndSort(String line) {
        String[] elems = line.split("\t");

        FacetFeedback ff;
        if (elems.length == 2) {
            ff = parseTermsAndSort(elems[1]);
        } else {
            ff = new FacetFeedback();
            ff.terms = new ArrayList<>();
            ff.facets = new ArrayList<>();
        }
        ff.qid = elems[0].split("-")[0];
        ff.sid = elems[0].split("-")[1];
        return ff;
    }

    @Override
    public String toString() {
        Collections.sort(terms);
        return qid + "-" + sid + "\t" + TextProcessing.join(terms, "|");
    }

    public static FacetFeedback parseTermsAndSort(String termsStr) {
        FacetFeedback ff = new FacetFeedback();
        ff.terms = new ArrayList<>();
        ff.facets = new ArrayList<>();
        if (termsStr.isEmpty()) {
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

    public static String toExpansionString(List<FeedbackTerm> selected) {
        java.util.Collections.sort(selected);
        return TextProcessing.join(selected, "|");
    }

    public static FacetFeedback parseFromExpansionString(String expansion) {
        return parseTermsAndSort(expansion);
    }

    public static FacetFeedback parseTermsFromUniqueExpansionString(String uniqueExpansionString) {
        return parseTermsAndSort(uniqueExpansionString.replace('~', '-'));
    }

    public static HashSet<String> loadFeedbackQidSidSet(File file) throws IOException {
        HashSet<String> set = new HashSet<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            FacetFeedback ff = FacetFeedback.parseFromStringAndSort(line);
            set.add(ff.qid + "-" + ff.sid);
        }
        reader.close();
        return set;
    }

}
