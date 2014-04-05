/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.utility.TextProcessing;

/**
 *
 * @author wkong
 */
public class CandidateList extends edu.umass.ciir.fws.types.CandidateList {
    String [] items; // candidate list items
    
    public CandidateList(String qid, long docRank, String listType, String [] items) {
        this.qid = qid;
        this.docRank = docRank;
        this.listType = listType;
        this.items = items;
        this.itemList = TextProcessing.join(items, "|");
    }
    
    public boolean valid() {
        if (items.length > 1) {
            return true;
        } else {
            return false;
        }
    }
    
}
