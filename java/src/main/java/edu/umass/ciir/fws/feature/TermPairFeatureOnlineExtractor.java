/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.clustering.gm.ScoredProbItem;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.utility.Utility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author wkong
 */
public class TermPairFeatureOnlineExtractor extends TermPairFeatureExtractor {

    public HashMap<String, ItemPairFeatures> extract(List<CandidateList> clists, List<RankedDocument> docs, List<ScoredProbItem> probItems) {
        Utility.info("extracting facet term pair features");
        this.clists = clists;
        this.docs = docs;

        List<ScoredItem> items = new ArrayList<>();
        for (ScoredProbItem item : probItems) {
            items.add(item);
        }
        loadItemsAndSetIds(items);
        
        generateItemPairs();
        extractLengthDiff();
        extractListFreq();
        extractContextListSim();
        extractContextTextSim();

        return this.itemPairFeatures;
    }

}
