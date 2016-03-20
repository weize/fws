/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings;
import edu.umass.ciir.fws.clustering.gm.gmj.GmjParameterSettings;
import edu.umass.ciir.fws.clustering.lda.LdaParameterSettings;
import edu.umass.ciir.fws.clustering.plsa.PlsaParameterSettings;
import edu.umass.ciir.fws.clustering.qd.QdParameterSettings;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public abstract class ParameterSettings {

    public abstract List<ModelParameters> getFacetingSettings();

    public abstract List<ModelParameters> getClusteringSettings();

    public abstract List<ModelParameters> getTuningSettings(List<Long> tuneMetricIndices);

    public static List<ModelParameters> getEmptySettings() {
        ArrayList<ModelParameters> empty = new ArrayList<>();
        empty.add(new ModelParameters.EmptyParameters());
        return empty;
    }

    public static ParameterSettings instance(Parameters p, String model) {
        switch (model) {
            case "qd":
                return new QdParameterSettings(p);
            case "lda":
                return new LdaParameterSettings(p);
            case "plsa":
                return new PlsaParameterSettings(p);
            case "gmi":
                return new GmiParameterSettings(p);
            case "gmj":
                return new GmjParameterSettings(p);
            case "prm":
                return new PrmParameterSettings(p);
        }
        return null;
    }
}