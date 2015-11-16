/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.demo.search.GalagoSearchEngine;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class QDFacetRefiner implements FacetRefiner {

    public QDFacetRefiner(Parameters p, GalagoSearchEngine galago) {
    }

    @Override
    public List<ScoredFacet> refine(List<CandidateList> clists, List<RankedDocument> docs) {
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
