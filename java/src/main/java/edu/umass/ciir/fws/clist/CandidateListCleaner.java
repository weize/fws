/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Clean and filter raw candidate lists extracted.
 *
 * @author wkong
 */
public class CandidateListCleaner {

    Set<String> stopwords = new HashSet<>();

    public CandidateListCleaner(Parameters parameters) {
        // load stopwords
        if (parameters.containsKey("stopwordsFile")) {
            String stopwordsFile = parameters.getString("stopwordsFile");
            try {
                stopwords = Utility.readFileToStringSet(new File(stopwordsFile));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            System.err.println("using stopword file " + stopwordsFile);
        } else {
            stopwords = new HashSet<>();
        }
    }

    /**
     *
     * @param clist
     * @return return null if the candidate list is not valid.
     */
    public CandidateList clean(CandidateList clist) {
        ArrayList<String> itemsCleaned = new ArrayList<>();
        HashSet<String> itemCleanedSet = new HashSet(); // constinct
        for (String item : clist.items) {
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
            return new CandidateList(
                    clist.qid, clist.docRank, clist.docName, clist.listType, itemsCleaned);
        } else {
            return null;
        }
    }

    public List<CandidateList> clean(List<CandidateList> clists) {
        List<CandidateList> cleanedClists = new ArrayList<>();
        for (CandidateList clist : clists) {
            CandidateList clistClean = clean(clist);
            if (clistClean != null) {
                cleanedClists.add(clistClean);
            }
        }
        return cleanedClists;
    }

    private boolean isValidItem(String item) {
        if (item.length() < 1) {
            return false;
        }

        if (stopwords.contains(item)) {
            return false;
        }

        // number of words
        int length = item.split("\\s+").length;
        return length <= edu.umass.ciir.fws.clist.CandidateList.MAX_TERM_SIZE;
    }

    private boolean isValidItemList(List<String> items) {
        int size = items.size();

        return size > 1 && size <= 200;
    }
}
