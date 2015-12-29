/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings;
import edu.umass.ciir.fws.clustering.lda.LdaParameterSettings;
import edu.umass.ciir.fws.clustering.plsa.PlsaParameterSettings;
import edu.umass.ciir.fws.clustering.qd.QdParameterSettings;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public abstract class ParameterSettings {

    public abstract List<ModelParameters> getFacetingSettings();

    public abstract List<ModelParameters> getClusteringSettings();

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
        }
        return null;
    }

}

//            if (model.equals("plsa")) {
//                List<Long> topicNums = p.getAsList("plsaTopicNums");
//                List<Long> termNums = p.getAsList("plsaTermNums");
//
//                for (long topic : topicNums) {
//                    for (long term : termNums) {
//                        String newParams = Utility.parametersToString(topic, term);
//                        params.add(newParams);
//                    }
//                }
//            } else if (model.equals("lda")) {
//                List<Long> topicNums = p.getAsList("ldaTopicNums");
//                List<Long> termNums = p.getAsList("ldaTermNums");
//
//                for (long topic : topicNums) {
//                    for (long term : termNums) {
//                        String newParams = Utility.parametersToString(topic, term);
//                        params.add(newParams);
//                    }
//                }
//
//            } else if (model.equals("qd")) {
//                List<Double> qdDistanceMaxs = p.getAsList("qdDistanceMaxs");
//                List<Double> qdWebsiteCountMins = p.getAsList("qdWebsiteCountMins");
//                List<Double> qdItemRatios = p.getAsList("qdItemRatios");
//
//                for (double dx : qdDistanceMaxs) {
//                    for (double wc : qdWebsiteCountMins) {
//                        for (double ir : qdItemRatios) {
//                            String newParams = Utility.parametersToString(dx, wc, ir);
//                            params.add(newParams);
//                        }
//                    }
//                }
//            }
