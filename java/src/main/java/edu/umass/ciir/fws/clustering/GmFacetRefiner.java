/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.demo.search.GalagoSearchEngine;
import edu.umass.ciir.fws.feature.TermFeaturesOnlineExtractor;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class GmFacetRefiner implements FacetRefiner {
    Parameters p;
    GalagoSearchEngine galago;
    
    public GmFacetRefiner(Parameters p, GalagoSearchEngine galago) {
        this.p = p;
        this.galago = galago;
    }

    @Override
    public List<ScoredFacet> refine(List<CandidateList> clists, List<RankedDocument> docs) {
        TermFeaturesOnlineExtractor termFeatureExtractor = new TermFeaturesOnlineExtractor(galago, p);
        termFeatureExtractor.extract(clists, docs);
        return null;        
    }
    
}
