/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws2.pilot.facetquality;

import edu.umass.ciir.fws.feature.Features;

/**
 *
 * @author wkong
 */
public class FacetFeatures extends Features {

    public static final int _qPrecision = 0;
    public static final int _qRecall = 1;
    public static final int _qF1 = 2;
    public static final int _qWf1 = 3; // weighted F1, weight by #facetTerms and rating
    public static final int _tSize = 4;
    public static final int _tProbSum = 5;
    public static final int _tProbAvg = 6;
    public static final int _tProbMin = 7;
    public static final int _tProbMax = 8;
    public static final int _pIntraSize = 9;
    public static final int _pIntraProbSum = 10;
    public static final int _pIntraProbAvg = 11;
    public static final int _pIntraProbMin = 12;
    public static final int _pIntraProbMax = 13;
    public static final int _pInterSize = 14;
    public static final int _pInterProbSum = 15;
    public static final int _pInterProbAvg = 16;
    public static final int _pInterProbMin = 17;
    public static final int _pInterProbMax = 18;

    public static final int size = 19;
    public static final int fIdxStart = 4; // start idx of features

    public FacetFeatures() {
        features = new Object[size];
    }

}
