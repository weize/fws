/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws2.pilot.qperformance;

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
    public static final int _pIntraSize = 5;
    public static final int _pIntraProbSum = 6;
    public static final int _pIntraProbAvg = 7;
    public static final int _pIntraProbMin = 8;
    public static final int _pIntraProbMax = 9;
    public static final int _pInterSize = 10;
    public static final int _pInterProbSum = 11;
    public static final int _pInterProbAvg = 12;
    public static final int _pInterProbMin = 13;
    public static final int _pInterProbMax = 14;

    public static final int size = 15;

    public QueryFeatures(String qid, String query) {
        this.qid = qid;
        this.query = query;
        features = new Object[size];
    }

    public TfQueryParameters toTfQueryParameters() {
        TfQueryParameters qp = new TfQueryParameters(qid, query, featuresToString());
        return qp;
    }

    public static HashMap<String, QueryFeatures> loadQueryFeatures(File file) {
        HashMap<String, QueryFeatures> qf = new HashMap<>();

        return qf;
    }
}
