/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.types.CandidateList;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Clean and filter raw candidate lists extracted.
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.CandidateList")
@OutputClass(className = "edu.umass.ciir.fws.types.CandidateList")
public class CandidateListCleaner extends StandardStep<CandidateList, CandidateList> {

    Set<String> stopwords = new HashSet<>();

    public CandidateListCleaner(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        // load stopwords
        String stopwordsFile = p.getString("stopwordsFile");
        stopwords = Utility.readFileToStringSet(new File(stopwordsFile));

    }

    @Override
    public void process(CandidateList clist) throws IOException {
        ArrayList<String> itemsCleaned = new ArrayList<>();
        HashSet<String> itemCleanedSet = new HashSet(); // constinct
        String[] items = CandidateListParser.splitItemList(clist.itemList);
        for (String item : items) {
            String itemCleaned = TextProcessing.clean(item);
            if (itemCleanedSet.contains(itemCleaned)) {
                continue;
            }
            if (isValidItem(itemCleaned)) {
                itemsCleaned.add(itemCleaned);
                itemCleanedSet.add(itemCleaned);
            }
        }
        
        if (isValidItemList(itemsCleaned)) {
            String itemList = CandidateListParser.joinItemList(itemsCleaned);
            processor.process(new CandidateList(
                    clist.qid, clist.docRank, clist.listType, itemList));
        }

    }

    private boolean isValidItem(String item) {
        if (item.length() <= 2) {
            return false;
        } 
        
        if (stopwords.contains(item)) {
            return false;
        }
        
        // number of words
        int length = item.split("\\s+").length;
        if (length > 10) {
            return false;
        }
        
        return true;
    }

    private boolean isValidItemList(List<String> items) {
        int size = items.size();
        
        if (size > 1 && size <= 200) {
            return true;
        } else {
            return false;
        }
    }
}
