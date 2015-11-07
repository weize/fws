/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.demo.search;

import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.types.TfQuery;
import java.util.List;

/**
 *
 * @author wkong
 */
public interface SearchEngine {
    /**
     * 
     * @param query
     * @param top number of top results to return
     * @return 
     */
    public List<RankedDocument> getRankedDocuments(TfQuery query, int top);
    
}
