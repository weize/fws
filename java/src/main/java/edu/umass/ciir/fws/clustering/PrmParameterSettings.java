/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
class PrmParameterSettings extends ParameterSettings {

    List<Long> prmFbDocs;
    List<Long> prmFbTerms;

    public PrmParameterSettings(Parameters p) {
        this.prmFbDocs = p.getList("prmFbDocs");
        this.prmFbTerms = p.getList("prmFbTerms");
    }

    @Override
    public List<ModelParameters> getFacetingSettings() {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (long d : prmFbDocs) {
            for (long t : prmFbTerms) {
                paramList.add(new PrmFacetParameters(d, t));
            }
        }
        return paramList;
    }

    @Override
    public List<ModelParameters> getClusteringSettings() {
        return getFacetingSettings();
    }

    @Override
    public List<ModelParameters> getTuningSettings(List<Long> tuneMetricIndices) {
        return getFacetingSettings();
    }
    
    public static class PrmFacetParameters extends ModelParameters {
        long prmFbDocs;
        long prmFbTerms;
        
        public PrmFacetParameters(long prmFbDocs, long prmFbTerms) {
            this.prmFbDocs = prmFbDocs;
            this.prmFbTerms = prmFbTerms;
            packParamsAsArray(prmFbDocs, prmFbTerms);
        }

        public PrmFacetParameters(String paramsString) {
            this(Long.parseLong(splitParameters(paramsString)[0]),
                    Long.parseLong(splitParameters(paramsString)[1]));
        }
    }

}
