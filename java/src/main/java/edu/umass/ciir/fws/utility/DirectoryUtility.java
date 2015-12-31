/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.utility;

/**
 *
 * @author wkong
 */
public class DirectoryUtility {

    public static String getCluterDir(String facetRunDir, String model) {
        //facet-run/qd/cluster
        return Utility.getFileName(facetRunDir, model, "cluster");
    }

    public static String getFacetDir(String facetRunDir, String model) {
        return Utility.getFileName(facetRunDir, model, "facet");
    }
    
    public static String getFeatureDir(String facetRunDir, String model) {
        //facet-run/qd/feature
        return Utility.getFileName(facetRunDir, model, "feature");
    }

    public static String getClusterFilename(String clusterDir, String qid, String model, String paramStr) {
        String name = paramStr.isEmpty() ? String.format("%s.%s.cluster", qid, model) : String.format("%s.%s.%s.cluster", qid, model, paramStr);
        return Utility.getFileName(clusterDir, qid, name);
    }
    
    public static String getFacetFilename(String facetDir, String qid, String model, String paramStr) {
        String name = paramStr.isEmpty() ? String.format("%s.%s.facet", qid, model) : String.format("%s.%s.%s.facet", qid, model, paramStr);
        return Utility.getFileName(facetDir, qid, name);
    }

    public static String getGmiFoldClusterFilename(String gmiRunDir, String folderId, String qid, String paramStr) {
        return getClusterFilename(Utility.getFileName(gmiRunDir, folderId, "cluster"), qid, "gmi", paramStr);
    }
    
    public static String getGmiFoldFacetFilename(String gmiRunDir, String folderId, String qid, String paramStr) {
        return getFacetFilename(Utility.getFileName(gmiRunDir, folderId, "facet"), qid, "gmi", paramStr);  
    }

    public static String getGmPredictDir(String gmDir) {
        return Utility.getFileName(gmDir, "predict");
    }

    public static String getTrainDir(String gmDir) {
        return Utility.getFileName(gmDir, "train");
    }

    public static String getGmTermPredictFileName(String predictDir, String qid) {
        return Utility.getFileNameWithSuffix(predictDir, qid, qid, "t.predict");
    }

    public static String getGmTermPairPredictFileName(String predictDir, String qid) {
        return Utility.getFileNameWithSuffix(predictDir, qid, qid, "p.predict.gz");
    }

    public static String getModelRunDir(String facetRun, String model) {
        return Utility.getFileName(facetRun, model);
    }

    

    

}
