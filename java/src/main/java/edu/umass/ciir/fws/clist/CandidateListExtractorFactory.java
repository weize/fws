/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.types.TfQuery;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class CandidateListExtractorFactory {

    public static CandidateListExtractor instance(Parameters p) {
        String patternType = p.getAsString("listPattern");
        if (patternType.equals("text")) {
            return new CandidateListNLPExtractor();
        } else if (patternType.equals("html")) {
            return new CandidateListHtmlExtractor();
        } else if (patternType.equals("both")) {
            return new CandidateListHtmlTextExtractor();
        } else {
            return null;
        }
    }

    private static class CandidateListHtmlTextExtractor implements CandidateListExtractor {

        CandidateListNLPExtractor textExtractor;
        CandidateListHtmlExtractor htmlExtractor;

        public CandidateListHtmlTextExtractor() {
            textExtractor = new CandidateListNLPExtractor();
            htmlExtractor = new CandidateListHtmlExtractor();
        }

        @Override
        public List<CandidateList> extract(List<RankedDocument> documents, TfQuery query) {
            ArrayList<CandidateList> clists = new ArrayList<>();
            clists.addAll(htmlExtractor.extract(documents, query));
            clists.addAll(textExtractor.extract(documents, query));
            return clists;
        }
    }

}
