/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.qperformance;

import edu.umass.ciir.fws.feature.Features;
import edu.umass.ciir.fws.types.TfQueryParameters;
import java.io.File;
import java.util.HashMap;

/**
 *
 * @author wkong
 */
public class QueryFeatures extends Features {

    String query;
    String qid;

    public static final int _tSize = 0;
    public static final int _tProbSum = 1;
    public static final int _tProbAvg = 2;
    public static final int _tProbMin = 3;
    public static final int _tProbMax = 4;
    public static final int _tProbStd = 5;
    public static final int _pIntraSize = 6;
    public static final int _pIntraProbSum = 7;
    public static final int _pIntraProbAvg = 8;
    public static final int _pIntraProbMin = 9;
    public static final int _pIntraProbMax =10;
    public static final int _pIntraProbStd =11;
    public static final int _pInterSize = 12;
    public static final int _pInterProbSum = 13;
    public static final int _pInterProbAvg = 14;
    public static final int _pInterProbMin = 15;
    public static final int _pInterProbMax = 16;
    public static final int _pInterProbStd = 17;
    public static final int _tLlSum = 18; //
    public static final int _tLlAvg = 19; // avg loglikelihood
    public static final int _pLlSum = 20; //
    public static final int _pLlAvg = 21; // avg loglikelihood
    public static final int _llSum = 22; // avg loglikelihood
    public static final int _tR = 23; // avg loglikelihood
    public static final int _tF = 24; // avg loglikelihood
    public static final int _pR = 25; // avg loglikelihood
    public static final int _pF = 26; // avg loglikelihood
    public static final int _prf = 27; // avg loglikelihood
    public static final int _prfa2 = 28; // avg loglikelihood
    public static final int _prfb05 = 29; // avg loglikelihood
    
    //public static final int _tP = 19; //
    //    public static final int _pP = 22; // avg loglikelihood
    public static final int size = 30;

    public QueryFeatures(String qid, String query) {
        this.qid = qid;
        this.query = query;
        features = new Object[size];
    }

    public TfQueryParameters toTfQueryParameters() {
        TfQueryParameters qp = new TfQueryParameters(qid, query, featuresToString());
        return qp;
    }

//    public static HashMap<String, QueryFeatures> loadQueryFeatures(File file) {
//        HashMap<String, QueryFeatures> qf = new HashMap<>();
//
//        return qf;
//    }
}
