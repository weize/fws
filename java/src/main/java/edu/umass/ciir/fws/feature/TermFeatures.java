/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.utility.TextProcessing;

/**
 * Represents facet term features.
 * @author wkong
 */
public class TermFeatures {
    
    final static String version = "7";
    String term;
    Object[] features;

    // index of features
    public static final int _len = 0;
    public static final int _listTf = 1;
    public static final int _listDf = 2;
    public static final int _listSf = 3; // site freq
    public static final int _listHlTf = 4;
    public static final int _listHlDf = 5;
    public static final int _listHlSf = 6; // site freq
    public static final int _contentTf = 7;
    public static final int _contentDf = 8;
    public static final int _contentWDf = 9;
    public static final int _contentSf = 10; // site freq
    public static final int _titleTf = 11;
    public static final int _titleDf = 12;
    public static final int _titleSf = 13; // site freq
    public static final int _clueIDF = 14;
    public static final int _listQueryIDF = 15;
    public static final int _listQueryHlIDF = 16;
    public static final int _listDocIDF = 17;
    public static final int _listDocHlIDF = 18;
    public static final int _listListIDF = 19;
    public static final int _listListHlIDF = 20;
    public static final int _contentTFClueIDF = 21;
    public static final int _listHlTFListQueryHlIDF = 22;
    public static final int _listHlTFListDocHlIDF = 23;
    public static final int _listHlTFListListHlIDF = 24;
    public static final int _listTFListQueryIDF = 25;
    public static final int _listTFListDocIDF = 26;
    public static final int _listTFListListIDF = 27;
    public static final int size = 28;

    public TermFeatures(String term) {
        this.features = new Object[size];
        this.term = term;
    }

    public void setFeature(Object value, int idx) {
        this.features[idx] = value;
    }

    public Object getFeature(int idx) {
        return this.features[idx];
    }

    @Override
    public String toString() {
        return term
                + "\t"
                + TextProcessing.join(features, "\t");
    }
}
