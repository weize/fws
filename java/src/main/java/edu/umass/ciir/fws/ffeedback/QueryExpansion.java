/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.types.TfQueryExpansion;
import edu.umass.ciir.fws.types.TfQueryExpansionSubtopic;
import java.util.List;

/**
 *
 * @author wkong
 */
public class QueryExpansion {

    

    public String qid;
    public String model;
    public long expId;
    public String expansion; // expansion string, e.g. fidx-tidx:term
    public String oriQuery; // original query
    public String expQuery; // expanded query
    public String id; // id for query expansion
    
    public QueryExpansion(String qid, String oriQuery, String model, String expansion, ExpansionIdMap expIdMap) {
        this.qid = qid;
        this.model = model;
        this.oriQuery = oriQuery;
        this.expansion = expansion;
        this.expId = expIdMap.getId(qid, model, expansion);
        this.id = toId(qid, model, expId);
    }

    public QueryExpansion(String qid, String oriQuery, String model, String expansion, long expId) {
        this.qid = qid;
        this.oriQuery = oriQuery;
        this.model = model;
        this.expansion = expansion;
        this.expId = expId;
        this.id = toId(qid, model, expId);
    }

    public static String expandQuery(String originalQuery, String expansion, String model) {
        switch (model) {
            case "sts":
                return expandSingleTermSimple(originalQuery, expansion);
            case "stb":
                return expandSingleTermBoolean(originalQuery, expansion);
            case "fts":
                return expandFeedbackTermSimple(originalQuery, expansion);
            case "ffs":
                return expandFacetFeedbackSimple(originalQuery, expansion);
            case "ftor":
                return expandFeedbackTermOr(originalQuery, expansion);
        }
        return null;
    }

    private static String expandSingleTermSimple(String originalQuery, String expansion) {
        FeedbackTerm ft = FeedbackTerm.parseFromString(expansion);
        return String.format("#combine:0=0.6:1=0.4(#sdm( %s ) #combine( %s ))", originalQuery, ft.term);
    }

    private static String expandSingleTermBoolean(String originalQuery, String expansion) {
        FeedbackTerm ft = FeedbackTerm.parseFromString(expansion);
        return String.format("#require(#exist(#od:1( %s )) #sdm( %s )) ", ft.term, originalQuery);
    }
    
    private static String expandFeedbackTermOr(String originalQuery, String expansion) {
        FacetFeedback feedback = FacetFeedback.parseFromExpansionString(expansion);
        StringBuilder query = new StringBuilder();
        if (feedback.terms.isEmpty()) {
            return String.format("#sdm( %s )", originalQuery);
        } else {
            query.append("#require( #any( ");
            for (FeedbackTerm term : feedback.terms) {
                query.append(String.format(" #exist(#od:1( %s )) ", term.term));
            }
            query.append(String.format(") #sdm( %s ))", originalQuery));
        }
        return query.toString();
    }

    /**
     * #combine( query #combine( #combine(term1) #combine(term2) ...))
     *
     * @param text
     * @param feedback
     * @return
     */
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

    private static String expandFacetFeedbackSimple(String originalQuery, String expansion) {
        FacetFeedback feedback = FacetFeedback.parseFromExpansionString(expansion);
        StringBuilder query = new StringBuilder();
        if (feedback.terms.isEmpty()) {
            return String.format("#sdm( %s )", originalQuery);
        } else {
            query.append(String.format("#combine:0=0.8:1=0.2(#sdm( %s ) #combine( ", originalQuery));
            for (List<FeedbackTerm> terms : feedback.facets) {
                query.append("#combine( ");
                for (FeedbackTerm term : terms) {
                    query.append(String.format("#combine( %s ) ", term.term));
                }
                query.append(" )");
            }
            query.append("))");
        }
        return query.toString();
    }

    public String expand() {
        expQuery = expandQuery(oriQuery, expansion, model);
        return expQuery;
    }

    public static String toId(String qid, String model, long expId) {
        return qid + "-" + model + "-" + expId;
    }

    public static String toId(TfQueryExpansion qe) {
        return qe.qid + "-" + qe.model + "-" + qe.expId;
    }

    public static String toId(TfQueryExpansionSubtopic qes) {
        return qes.qid + "-" + qes.model + "-" + qes.expId;
    }

    public TfQueryExpansion toTfQueryExpansion() {
        return new TfQueryExpansion(qid, model, expId, expQuery, oriQuery, expansion);
    }

//    @Override
//    public String toString() {
//        return String.format("%s\t%s\t%s\t%d\n", qid, model, expansion, expId);
//    }
}
