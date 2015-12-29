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
        }
        return null;
    }
}

//                    if (model.equals("plsa") || model.equals("lda") || model.equals("qd")) {
//                        for (long idx : facetTuneMetricIndices) {
//                            String params = Utility.parametersToString(model, idx, topFacets);
//                            processor.process(new TfQueryParameters("0", "", params));
//                        }
//                    } else if (model.equals("gmj") || model.equals("gmc")) {
//                        for (String ranker : gmRankers) {
//                            String params = Utility.parametersToString(model, ranker, topFacets);
//                            processor.process(new TfQueryParameters("0", "", params));
//                        }
//                    } else if (model.equals("gmi")) {
//                        for (long idx : facetTuneMetricIndices) {
//                            for (String ranker : gmRankers) {
//                                String params = Utility.parametersToString(model, ranker, idx, topFacets);
//                                processor.process(new TfQueryParameters("0", "", params));
//                            }
//                        }
//
//                    } else if (model.equals("rerank")) {
//                        String params = Utility.parametersToString(model, topFacets);
//                        processor.process(new TfQueryParameters("0", "", params));
//                    } else {
//                        throw new IOException("cannot recognize " + model);
//                    }


//            if (model.equals("plsa") || model.equals("lda") || model.equals("qd")) {
//                String optMetricIdx = params[1];
//                facetParam = optMetricIdx;
//
//            } else if (model.equals("gmj") || model.equals("gmc")) {
//                String ranker = params[1];
//                facetParam = ranker;
//            } else if (model.equals("gmi")) {
//
//                String ranker = params[1];
//                String optMetricIdx = params[2];
//                facetParam = Utility.parametersToFileNameString(ranker, optMetricIdx);
//            } else if (model.equals("rerank")) {
//                facetParam = "";
//            } else {
//                throw new IOException("cannot recognize " + model);
//            }

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
