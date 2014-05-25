/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.types.TfQueryExpansion;

/**
 *
 * @author wkong
 */
public class QueryExpansion {

    public String qid;
    public String model;
    public Long expId;
    public String expansion;
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
                return SingleTermSimple(originalQuery, expansion);
        }
        return null;
    }

    private static String SingleTermSimple(String originalQuery, String expansion) {
        FeedbackTerm ft = FeedbackTerm.parseFromString(expansion);
        return String.format("#combine:0=0.6:1=0.4(#sdm( %s ) #combine( %s ))", originalQuery, ft.term);
    }

    QueryExpansion(String qid, String oriQuery, String model, String expansion, ExpansionIdMap2 expIdMap) {
        this.qid = qid;
        this.model = model;
        this.oriQuery = oriQuery;
        this.expansion = expansion;
        this.expId = expIdMap.getId(qid, model, expansion);
    }

    public String expand() {
        expQuery = expandQuery(oriQuery, expansion, model);
        return expQuery;
    }

    public String toName() {
        return qid + "-" + model + "-" + expId;
    }

    public static String toName(TfQueryExpansion qe) {
        return qe.qid + "-" + qe.model + "-" + qe.expId;
    }

    public TfQueryExpansion toTfQueryExpansion() {
        return new TfQueryExpansion(qid, model, expId, expQuery);
    }

    @Override
    public String toString() {
        return String.format(model, qid, model, expId, expansion);
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
}
