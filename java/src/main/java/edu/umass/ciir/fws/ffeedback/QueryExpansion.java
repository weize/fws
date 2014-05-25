/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.types.TfQueryExpansion;
import edu.umass.ciir.fws.types.TfQueryExpansionSubtopic;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author wkong
 */
public class QueryExpansion {

    public String qid;
    public String model;
    public Long expId;
    public String expansion; // expansion string, e.g. fidx-tidx:term
    public String oriQuery; // original query
    public String expQuery; // expanded query

    public QueryExpansion(String qid, String oriQuery, String model, String expansion) {
        this.qid = qid;
        this.oriQuery = oriQuery;
        this.model = model;
        this.expansion = expansion;
    }

    public static String expandQuery(String originalQuery, String expansion, String model) {
        switch (model) {
            case "sts":
                return expandSingleTermSimple(originalQuery, expansion);
            case "fts":
                return expandFeedbackTermSimple(originalQuery, expansion);
        }
        return null;
    }

    private static String expandSingleTermSimple(String originalQuery, String expansion) {
        FeedbackTerm ft = FeedbackTerm.parseFromString(expansion);
        return String.format("#combine:0=0.6:1=0.4(#sdm( %s ) #combine( %s ))", originalQuery, ft.term);
    }

    private static String expandFeedbackTermSimple(String originalQuery, String expansion) {
        FacetFeedback feedback = FacetFeedback.parseFromExpansionString(expansion);
        StringBuilder query = new StringBuilder();
        if (feedback.terms.isEmpty()) {
            return String.format("#sdm( %s )", originalQuery);
        } else {
            query.append(String.format("#combine:0=0.8:1=0.2(#sdm( %s ) #combine( ", originalQuery));
            for (FeedbackTerm term : feedback.terms) {
                query.append(String.format("#combine( %s ) ", term.term));
            }
            query.append("))");
        }
        return query.toString();
    }

    public QueryExpansion(String qid, String oriQuery, String model, String expansion, ExpansionIdMap expIdMap) {
        this.qid = qid;
        this.model = model;
        this.oriQuery = oriQuery;
        this.expansion = expansion;
        this.expId = expIdMap.getId(qid, model, expansion);
    }

    public QueryExpansion(String qid, String model, Long expId, String expansion) {
        this.qid = qid;
        this.model = model;
        this.expansion = expansion;
        this.expId = expId;
    }

    public String expand() {
        expQuery = expandQuery(oriQuery, expansion, model);
        return expQuery;
    }

    public String toName() {
        return qid + "-" + model + "-" + expId;
    }
    
    public static String toName(String qid, String model, long expId) {
        return qid + "-" + model + "-" + expId;
    }

    public static String toName(TfQueryExpansion qe) {
        return qe.qid + "-" + qe.model + "-" + qe.expId;
    }

    public static String toName(TfQueryExpansionSubtopic qes) {
        return qes.qid + "-" + qes.model + "-" + qes.expId;
    }

    public TfQueryExpansion toTfQueryExpansion() {
        return new TfQueryExpansion(qid, model, expId, expQuery);
    }

    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%d\n", qid, model, expansion, expId);
    }

    public static QueryExpansion parseQExpansion(String text) {
        String[] elems = text.split("\t");
        String qid = elems[0];
        String model = elems[1];
        String expansion = elems[2]; // expansion may be empty
        Long expId = Long.parseLong(elems[3]);
        return new QueryExpansion(qid, model, expId, expansion);
    }

    /**
     * #combine( query #combine( #combine(term1) #combine(term2) ...))
     *
     * @param text
     * @param feedback
     * @return
     */
    private String expandSdmQuery(String text, FacetFeedback feedback) {
        StringBuilder query = new StringBuilder();
        if (feedback.terms.isEmpty()) {
            return String.format("#sdm( %s )", text);
        } else {
            query.append(String.format("#combine:0=0.8:1=0.2(#sdm( %s ) #combine( ", text));
            for (FeedbackTerm term : feedback.terms) {
                query.append(String.format("#combine( %s ) ", term.term));
            }
            query.append("))");
        }
        return query.toString();
    }

    /**
     * qid-model-expId -> feedbackTerm
     * @param file a expansion file
     * @return
     * @throws IOException
     */
    public static HashMap<String, FeedbackTerm> loadExpansionTermsAsMap(File file, String model) throws IOException {
        BufferedReader reader = Utility.getReader(file);
        HashMap<String, FeedbackTerm> map = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("#")) {
                continue;
            }
            QueryExpansion qe = QueryExpansion.parseQExpansion(line);
            if (qe.model.equals(model)) {
                FeedbackTerm ft = FeedbackTerm.parseFromString(qe.expansion);
                map.put(qe.toName(), ft);
            }
        }
        reader.close();
        return map;
    }
}
