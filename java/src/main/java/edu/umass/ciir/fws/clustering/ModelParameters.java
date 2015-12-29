/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.clustering.qd.QdParameterSettings;
import edu.umass.ciir.fws.utility.Utility;

/**
 *
 * @author wkong
 */
public abstract class ModelParameters {

    public Object[] paramArray;

    public String toFilenameString() {
        return Utility.parametersToFileNameString(paramArray);
    }

    @Override
    public String toString() {
        return Utility.parametersToString(paramArray);
    }

    public final static String[] splitParameters(String paramsString) {
        return Utility.splitParameters(paramsString);
    }

    public final static Object[] packParamsAsArray(Object... parameters) {
        Object[] params = new Object[parameters.length];
        int i = 0;
        for (Object param : parameters) {
            params[i++] = param;
        }
        return params;
    }
    
    public static ModelParameters parseFacetParams(String paramString, String model) {
        switch (model) {
            case "qd":
                return new QdParameterSettings.FacetParameters(paramString);
        }
        return null;
    }
}


//        if (model.equals("plsa")) {
//            long topicNum = Long.parseLong(params[2]);
//            long termNum = Long.parseLong(params[3]);
//            param = Utility.parametersToFileNameString(topicNum, termNum);
//        } else if (model.equals("lda")) {
//            long topicNum = Long.parseLong(params[2]);
//            long termNum = Long.parseLong(params[3]);
//            param = Utility.parametersToFileNameString(topicNum, termNum);
//
//        } else if (model.equals("qd")) {
//            double qdDistanceMax = Double.parseDouble(params[2]);
//            double qdWebsiteCountMin = Double.parseDouble(params[3]);
//            double qdItemRatio = Double.parseDouble(params[4]);
//            param = Utility.parametersToFileNameString(qdDistanceMax, qdWebsiteCountMin, qdItemRatio);
//        }

