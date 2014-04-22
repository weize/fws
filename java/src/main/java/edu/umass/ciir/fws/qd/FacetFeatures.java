/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.qd;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.utility.TextProcessing;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Facet feature for QD (Query Dimensions)
 * @author wkong
 */
public class FacetFeatures extends CandidateList{

    Object[] features;
    // other index are same as KeyphraseFeatureExtend
    public static final int _len = 0;
    public static final int _WDF = 1;
    public static final int _cluIDF = 2;
    public static final int _qdScore = 3;
    public static final int _sites = 4;
    public static final int size = 5;
    public String[] sites;

    public FacetFeatures(CandidateList clist) {
        this.listType = clist.listType;
        this.docRank = clist.docRank;
        this.qid = clist.qid;
        this.itemList = clist.itemList;
        this.items = Arrays.copyOf(clist.items, clist.items.length);
        this.features = new Object[size];
    }

//    public String getKeyphrasesId() {
//        int len = keyphrases.length;
//        String[] keyphrasesSorted = new String[len];
//        for (int i = 0; i < len; i++) {
//            keyphrasesSorted[i] = keyphrases[i].text;
//        }
//        Arrays.sort(keyphrasesSorted);
//        return TextProcessing.join(keyphrasesSorted, "|");
//    }

    public void setFeature(Object value, int idx) {
        this.features[idx] = value;
    }

    public Object getFeature(int idx) {
        return this.features[idx];
    }

//    public static FacetFeatures[] readListsFromFile(String filename) throws IOException {
//        ArrayList<FacetFeatures> lfs = new ArrayList<FacetFeatures>();
//
//        BufferedReader in = new BufferedReader(new FileReader(filename));
//        String line;
//        while ((line = in.readLine()) != null) {
//            lfs.add(readListFromString(line));
//        }
//        in.close();
//        return lfs.toArray(new FacetFeatures[0]);
//    }
//    
//    public static FacetFeatures[] readListsFromFileNotTr(String filename) throws IOException {
//        ArrayList<FacetFeatures> lfs = new ArrayList<FacetFeatures>();
//
//        BufferedReader in = new BufferedReader(new FileReader(filename));
//        String line;
//        while ((line = in.readLine()) != null) {
//            FacetFeatures  list= readListFromString(line);
//            if (!list.type.equalsIgnoreCase("tr")) {
//                lfs.add(list);
//            }
//        }
//        in.close();
//        return lfs.toArray(new FacetFeatures[0]);
//    }
//    
//
//    public static FacetFeatures readListFromString(String line) {
//        String[] elems = line.split("\\t");
//        FacetFeatures list = new FacetFeatures(AspectHtmlList.readListFromString(line));
//
//        // features: qid docid type terms len ... sites
//        // len 
//        list.setFeature(Integer.parseInt(elems[4]), FacetFeatures._len);
//        // others
//        for (int i = 5; i < elems.length; i++) {
//            int index = i - 4;
//            Object value;
//            if (index == FacetFeatures._sites) {
//                list.sites = elems[i].split("\\|");
//                value = elems[i];
//            } else {
//                value = Double.parseDouble(elems[i]);
//            }
//            list.setFeature(value, index);
//        }
//        // sites
//
//
//        return list;
//    }

    @Override
    public String toString() {
        return super.toString()
                + "\t" + TextProcessing.join(features, "\t");
    }
}
