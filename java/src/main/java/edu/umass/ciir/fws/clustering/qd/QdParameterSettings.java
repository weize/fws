/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clustering.ModelParameters;
import static edu.umass.ciir.fws.clustering.ModelParameters.packParamsAsArray;
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
    public List<ModelParameters> getFacetingSettings() {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (double distanceMax : distanceMaxs) {
            for (double websiteCountMin : websiteCountMins) {
                for (double itemRatio : itemRatios) {
                    for (double itemThreshld : itemThreshlds) {
                        paramList.add(new QdFacetParameters(distanceMax, websiteCountMin, itemRatio, itemThreshld));
                    }
                }
            }
        }
        return paramList;
    }

    @Override
    public List<ModelParameters> getClusteringSettings() {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (double distanceMax : distanceMaxs) {
            for (double websiteCountMin : websiteCountMins) {
                paramList.add(new QdClusterParameters(distanceMax, websiteCountMin));
            }
        }
        return paramList;
    }

    @Override
    public List<ModelParameters> getTuningSettings(List<Long> tuneMetricIndices) {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (long idx : tuneMetricIndices) {
            paramList.add(new QdTuneParameters(idx));
        }
        return paramList;
    }

    public static class QdTuneParameters extends ModelParameters {

        long metricIndex;

        public QdTuneParameters(long metricIndex) {
            this.metricIndex = metricIndex;
            this.paramArray = packParamsAsArray(metricIndex);
        }

        public QdTuneParameters(String paramsString) {
            this(Long.parseLong(paramsString));
        }

    }

    public static class QdClusterParameters extends ModelParameters {

        public double distanceMax;
        public double websiteCountMin;

        public QdClusterParameters(double distanceMax, double websiteCountMin) {
            this.distanceMax = distanceMax;
            this.websiteCountMin = websiteCountMin;
            this.paramArray = packParamsAsArray(distanceMax, websiteCountMin);
        }

        // should have easier way to do this
        public QdClusterParameters(String paramsString) {
            this(Double.parseDouble(splitParameters(paramsString)[0]),
                    Double.parseDouble(splitParameters(paramsString)[1]));
        }

    }

    public static class QdFacetParameters extends ModelParameters {

        double distanceMax;
        double websiteCountMin;
        double itemRatio;
        double itemThreshld;

        public QdFacetParameters(double distanceMax, double websiteCountMin, double itemRatio, double itemThreshld) {
            this.distanceMax = distanceMax;
            this.websiteCountMin = websiteCountMin;
            this.itemRatio = itemRatio;
            this.itemThreshld = itemThreshld;
            this.paramArray = packParamsAsArray(distanceMax, websiteCountMin, itemRatio, itemThreshld);
        }

        public QdFacetParameters(String paramsString) {
            this(Double.parseDouble(splitParameters(paramsString)[0]),
                    Double.parseDouble(splitParameters(paramsString)[1]),
                    Double.parseDouble(splitParameters(paramsString)[2]),
                    Double.parseDouble(splitParameters(paramsString)[3]));
        }

    }

}
