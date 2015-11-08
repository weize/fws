/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.utility.TextProcessing;

/**
 * Represents facet term features.
 *
 * @author wkong
 */
public class TermFeatures extends Features{
    static final String version = "8";
    String term;
    // index of features
    public static final int _len = 0; // number of tokens
    public static final int _listTf = 1; // tf
    public static final int _listDf = 2; // docs contains the term in candidate list
    public static final int _listSf = 3; // site freq
    // for <ul> pattern
    public static final int _listUlTf = 4;
    public static final int _listUlDf = 5;
    public static final int _listUlSf = 6;
    // for <ol> pattern
    public static final int _listOlTf = 7;
    public static final int _listOlDf = 8;
    public static final int _listOlSf = 9;
    // for <select> pattern
    public static final int _listSlTf = 10;
    public static final int _listSlDf = 11;
    public static final int _listSlSf = 12;
    // for <tr> pattern
    public static final int _listTrTf = 13;
    public static final int _listTrDf = 14;
    public static final int _listTrSf = 15;
    // for <td> pattern
    public static final int _listTdTf = 16;
    public static final int _listTdDf = 17;
    public static final int _listTdSf = 18;
    // for text pattern
    public static final int _listTxTf = 19;
    public static final int _listTxDf = 20;
    public static final int _listTxSf = 21;
    // title
    public static final int _titleTf = 22;
    public static final int _titleDf = 23;
    public static final int _titleSf = 24;
    // content
    public static final int _contentTf = 25;
    public static final int _contentDf = 26;
    public static final int _contentWDf = 27;
    public static final int _contentSf = 28;
    // idf
    public static final int _clueIDF = 29;
    public static final int _listIDF = 30;
    // tf.idf
    public static final int _contentTFClueIDF = 31;
    public static final int _listTFListIDF = 32;
    public static final int size = 33;

    public static int[] getIndicesForListTFs(String clistListType) {
        switch (clistListType) {
            case "all":
                return new int[]{_listTf, _listDf, _listSf};
            case "ul":
                return new int[]{_listUlTf, _listUlDf, _listUlSf};
            case "ol":
                return new int[]{_listOlTf, _listOlDf, _listOlSf};
            case "select":
                return new int[]{_listSlTf, _listSlDf, _listSlSf};
            case "tr":
                return new int[]{_listTrTf, _listTrDf, _listTrSf};
            case "td":
                return new int[]{_listTdTf, _listTdDf, _listTdSf};
            case "tx":
                return new int[]{_listTxTf, _listTxDf, _listTxSf};
        }
        return null;
    }

    public TermFeatures(String term) {
        this.features = new Object[size];
        this.term = term;
    }

    @Override
    public String toString() {
        return term + "\t" + TextProcessing.join(features, "\t");
    }
    
}
