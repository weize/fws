/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import java.util.List;

/**
 *
 * @author wkong
 */
public class GmCluster extends ScoredFacet {
    Integer [] nodeIds;
    
    public GmCluster(List<Integer> nodeIdList, double score) {
        nodeIds = nodeIdList.toArray(new Integer[0]);
        this.score = score;
    }
    
    public GmCluster(List<Integer> nodeIdList) {
        nodeIds = nodeIdList.toArray(new Integer[0]);
    }
}
