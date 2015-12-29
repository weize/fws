/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.ParameterSettings;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class QdParameterSettings extends ParameterSettings {

    List<Double> distanceMaxs;
    List<Double> websiteCountMins;
    List<Double> itemRatios;
    List<Double> itemThreshlds;

    public QdParameterSettings(Parameters p) {
        distanceMaxs = p.getList("qdDistanceMaxs");
        websiteCountMins = p.getList("qdWebsiteCountMins");
        itemRatios = p.getList("qdItemRatios");
        itemThreshlds = p.getList("qdItemThresholds");
    }

    @Override
    public List<ModelParameters> getFacetParametersList() {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (double distanceMax : distanceMaxs) {
            for (double websiteCountMin : websiteCountMins) {
                for (double itemRatio : itemRatios) {
                    for (double itemThreshld : itemThreshlds) {
                        paramList.add(new FacetParameters(distanceMax, websiteCountMin, itemRatio, itemThreshld));
                    }
                }
            }
        }
        return paramList;
    }

    @Override
    public List<ModelParameters> getClusterParametersList() {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (double distanceMax : distanceMaxs) {
            for (double websiteCountMin : websiteCountMins) {
                paramList.add(new ClusterParameters(distanceMax, websiteCountMin));
            }
        }
        return paramList;
    }

    public static class ClusterParameters extends ModelParameters {

        public double distanceMax;
        public double websiteCountMin;

        public ClusterParameters(double distanceMax, double websiteCountMin) {
            this.distanceMax = distanceMax;
            this.websiteCountMin = websiteCountMin;
            this.paramArray = packParamsAsArray(distanceMax, websiteCountMin);
        }

        // should have easier way to do this
        public ClusterParameters(String paramsString) {
            this(Double.parseDouble(splitParameters(paramsString)[0]),
                    Double.parseDouble(splitParameters(paramsString)[1]));
        }

    }

    public static class FacetParameters extends ModelParameters {

        double distanceMax;
        double websiteCountMin;
        double itemRatio;
        double itemThreshld;

        public FacetParameters(double distanceMax, double websiteCountMin, double itemRatio, double itemThreshld) {
            this.distanceMax = distanceMax;
            this.websiteCountMin = websiteCountMin;
            this.itemRatio = itemRatio;
            this.itemThreshld = itemThreshld;
            this.paramArray = packParamsAsArray(distanceMax, websiteCountMin, itemRatio, itemThreshld);
        }

        public FacetParameters(String paramsString) {
            this(Double.parseDouble(splitParameters(paramsString)[0]),
                    Double.parseDouble(splitParameters(paramsString)[1]),
                    Double.parseDouble(splitParameters(paramsString)[2]),
                    Double.parseDouble(splitParameters(paramsString)[3]));
        }

    }

}
