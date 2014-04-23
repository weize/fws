/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.qd;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clist.CandidateListParser;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Facet feature for QD (Query Dimensions)
 *
 * @author wkong
 */
public class FacetFeatures extends CandidateList implements Comparable<FacetFeatures> {

    Object[] features;
    // other index are same as KeyphraseFeatureExtend
    public static final int _len = 0;
    public static final int _WDF = 1;
    public static final int _cluIDF = 2;
    public static final int _qdScore = 3;
    public static final int _sites = 4; // a string lists all sites that contains this list (all items in the list)
    public static final int size = 5; // feature size
    public String[] sites;

    public FacetFeatures(CandidateList clist) {
        this.listType = clist.listType;
        this.docRank = clist.docRank;
        this.qid = clist.qid;
        this.itemList = clist.itemList;
        this.items = Arrays.copyOf(clist.items, clist.items.length);
        this.features = new Object[size];
    }

    public FacetFeatures() {

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

    public static List<FacetFeatures> readFromFile(String filename) throws IOException {
        ArrayList<FacetFeatures> ffs = new ArrayList<>();
        BufferedReader in = Utility.getReader(filename);
        String line;
        System.out.println(filename);
        while ((line = in.readLine()) != null) {
            String[] fields = line.split("\t");
            // features: qid docrank type terms len ... sites
            FacetFeatures ff = new FacetFeatures();
            ff.qid = fields[0];
            ff.docRank = Long.parseLong(fields[1]);
            ff.listType = fields[2];
            ff.itemList = fields[3];
            ff.items = CandidateListParser.splitItemList(ff.itemList);
            ff.features = new Object[size];
            ff.features[_len] = Integer.parseInt(fields[4]);
            ff.features[_WDF] = Double.parseDouble(fields[5]);
            ff.features[_cluIDF] = Double.parseDouble(fields[6]);
            ff.features[_qdScore] = Double.parseDouble(fields[7]);
            if (fields.length < 9) {
                System.out.println(line);
            }
            ff.features[_sites] = fields[8];
            ff.sites = splitStringToSites(fields[8]);
            ffs.add(ff);
        }
        in.close();
        return ffs;
    }

    public static String joinSitesToString(String[] sites) {
        return TextProcessing.join(sites, "|");
    }

    public static String[] splitStringToSites(String sitesStr) {
        return sitesStr.split("\\|");
    }

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
//        return list;
//    }
    @Override
    public String toString() {
        return super.toString()
                + "\t" + TextProcessing.join(features, "\t");
    }

    @Override
    public int compareTo(FacetFeatures other) {
        return Utility.compare(other.getScore(), this.getScore());
    }
    
    public double getScore() {
        return (Double) this.features[_qdScore];
    }
    
    public int getItemsLen() {
        return items.length;
    }
}
