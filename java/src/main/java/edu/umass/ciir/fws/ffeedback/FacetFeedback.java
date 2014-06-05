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
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

    public FacetFeedback(String qid, String sid, ArrayList<FeedbackTerm> terms) {
        this.qid = qid;
        this.sid = sid;
        Collections.sort(terms);
        this.terms = terms;
    }

    private FacetFeedback() {

    }

    public String termsToString() {
        return TextProcessing.join(terms, "|");
    }

    public static FacetFeedback parse(String line) {
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

    @Override
    public String toString() {
        return qid + "-" + sid + "\t" + TextProcessing.join(terms, "|");
    }

    public static FacetFeedback parseTerms(String termsStr) {
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
        return parseTerms(expansion);
    }

    public static List<FacetFeedback> load(File file) throws IOException {
        ArrayList<FacetFeedback> list = new ArrayList<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            FacetFeedback ff = FacetFeedback.parse(line);
            list.add(ff);
        }
        reader.close();
        return list;
    }

    public static HashMap<String, List<FacetFeedback>> loadGroupByQid(File file) throws IOException {
        List<FacetFeedback> all = load(file);
        HashMap<String, List<FacetFeedback>> map = new HashMap<>();
        for (FacetFeedback ff : all) {
            if (!map.containsKey(ff.qid)) {
                map.put(ff.qid, new ArrayList<FacetFeedback>());
            }
            map.get(ff.qid).add(ff);
        }
        return map;
    }

    public static HashSet<String> loadFeedbackQidSidSet(File file) throws IOException {
        HashSet<String> set = new HashSet<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            FacetFeedback ff = FacetFeedback.parse(line);
            set.add(ff.qid + "-" + ff.sid);
        }
        reader.close();
        return set;
    }

    public static FacetFeedback getSimulatedFfeedback(FacetFeedback feedbackSource, List<ScoredFacet> facets) {
        HashSet<String> selected = new HashSet<>();
        for (FeedbackTerm term : feedbackSource.terms) {
            selected.add(term.term);
        }

        ArrayList<FeedbackTerm> fterms = new ArrayList<>();
        for (int fidx = 0; fidx < facets.size(); fidx++) {
            List<ScoredItem> items = facets.get(fidx).items;
            for (int tidx = 0; tidx < items.size(); tidx++) {
                String term = items.get(tidx).item;
                if (selected.contains(term)) {
                    fterms.add(new FeedbackTerm(term, fidx, tidx));
                    selected.remove(term);
                }
            }
        }

        return new FacetFeedback(feedbackSource.qid, feedbackSource.sid, fterms);
    }

}
