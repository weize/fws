/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

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
        rankers = p.getAsList("rankers");
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

    public static class GmiFacetParameters extends ModelParameters {

        double termProbTh;
        double pairProbTh;
        String ranker;

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
