/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmi;

import edu.umass.ciir.fws.clustering.ModelParameters;
import static edu.umass.ciir.fws.clustering.ModelParameters.packParamsAsArray;
import static edu.umass.ciir.fws.clustering.ModelParameters.splitParameters;
import edu.umass.ciir.fws.clustering.ParameterSettings;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class GmiParameterSettings extends ParameterSettings {

    List<Double> termProbThs;
    List<Double> pairProbThs;
    List<String> rankers;

    public GmiParameterSettings(Parameters p) {
        termProbThs = p.getAsList("gmiTermProbThesholds");
        pairProbThs = p.getAsList("gmiPairProbThesholds");
        rankers = p.getAsList("gmRankers");
    }

    @Override
    public List<ModelParameters> getFacetingSettings() {
        ArrayList<ModelParameters> params = new ArrayList<>();
        for (double termTh : termProbThs) {
            for (double pairTh : pairProbThs) {
                for (String ranker : rankers) {
                    params.add(new GmiFacetParameters(termTh, pairTh, ranker));
                }
            }
        }
        return params;
    }

    @Override
    public List<ModelParameters> getClusteringSettings() {
        ArrayList<ModelParameters> params = new ArrayList<>();
        for (double termTh : termProbThs) {
            for (double pairTh : pairProbThs) {
                params.add(new GmiClusterParameters(termTh, pairTh));
            }
        }
        return params;
    }

    @Override
    public List<ModelParameters> getTuningSettings(List<Long> tuneMetricIndices) {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (String ranker : rankers) {
            for (long idx : tuneMetricIndices) {
                paramList.add(new GmiTuneParameters(ranker, idx));
            }
        }
        return paramList;
    }

    public static class GmiTuneParameters extends ModelParameters {

        String ranker;
        long metricIndex;

        public GmiTuneParameters(String ranker, long metricIndex) {
            this.ranker = ranker;
            this.metricIndex = metricIndex;
            this.paramArray = packParamsAsArray(ranker, metricIndex);
        }

        public GmiTuneParameters(String paramsString) {
            this(splitParameters(paramsString)[0],
                    Long.parseLong(splitParameters(paramsString)[1]));
        }

    }

    public List<ModelParameters> appendFacetSettings(GmiClusterParameters clusterParams) {
        ArrayList<ModelParameters> facetParams = new ArrayList<>();
        for (String ranker : rankers) {
            facetParams.add(new GmiFacetParameters(clusterParams.termProbTh, clusterParams.pairProbTh, ranker));
        }
        return facetParams;
    }

    public static class GmiFacetParameters extends ModelParameters {

        public double termProbTh;
        public double pairProbTh;
        public String ranker;

        public GmiFacetParameters(double termProbTh, double pairProbTh, String ranker) {
            this.termProbTh = termProbTh;
            this.pairProbTh = pairProbTh;
            this.ranker = ranker;
            packParamsAsArray(termProbTh, pairProbTh, ranker);
        }

        public GmiFacetParameters(String paramsString) {
            this(Double.parseDouble(splitParameters(paramsString)[0]),
                    Double.parseDouble(splitParameters(paramsString)[1]),
                    splitParameters(paramsString)[2]);
        }
    }

    public static class GmiClusterParameters extends ModelParameters {

        double termProbTh;
        double pairProbTh;

        public GmiClusterParameters(double termProbTh, double pairProbTh) {
            this.termProbTh = termProbTh;
            this.pairProbTh = pairProbTh;
            packParamsAsArray(termProbTh, pairProbTh);
        }

        public GmiClusterParameters(String paramsString) {
            this(Double.parseDouble(splitParameters(paramsString)[0]),
                    Double.parseDouble(splitParameters(paramsString)[1]));
        }
    }

}
