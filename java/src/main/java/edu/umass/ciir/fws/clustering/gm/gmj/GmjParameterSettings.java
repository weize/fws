/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmj;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.ParameterSettings;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class GmjParameterSettings extends ParameterSettings {

    List<String> rankers;

    public GmjParameterSettings(Parameters p) {
        rankers = p.getAsList("gmRankers");
    }

    @Override
    public List<ModelParameters> getFacetingSettings() {
        ArrayList<ModelParameters> paramsList = new ArrayList<>();
        for(String ranker : rankers) {
            paramsList.add(new GmjFacetParameters(ranker));
        }
        return paramsList;
    }

    @Override
    public List<ModelParameters> getClusteringSettings() {
        return getEmptySettings();
    }

    @Override
    public List<ModelParameters> getTuningSettings(List<Long> tuneMetricIndices) {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (String ranker : rankers) {
            paramList.add(new GmjTuneParameters(ranker));
        }
        return paramList;
    }

    public static class GmjFacetParameters extends ModelParameters {

        String ranker;

        public GmjFacetParameters(String ranker) {
            this.ranker = ranker;
            packParamsAsArray(ranker);
        }
    }
    
    public static class GmjTuneParameters extends ModelParameters {

        String ranker;

        public GmjTuneParameters(String ranker) {
            this.ranker = ranker;
            packParamsAsArray(ranker);
        }
    }

}
