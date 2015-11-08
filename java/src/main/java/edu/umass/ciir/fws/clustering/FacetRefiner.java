/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import java.util.List;

/**
 *
 * @author wkong
 */
public interface FacetRefiner {
    public List<ScoredFacet> refine(List<CandidateList> clists, List<RankedDocument> docs);
    
}
