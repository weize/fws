/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.lda;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.ParameterSettings;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class LdaParameterSettings extends ParameterSettings {

    List<Long> ldaTopicNums;
    List<Long> ldaTermNums;

    public LdaParameterSettings(Parameters p) {
        ldaTopicNums = p.getList("ldaTopicNums");
        ldaTermNums = p.getList("ldaTermNums");
    }

    @Override
    public List<ModelParameters> getFacetingSettings() {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (long topicNum : ldaTopicNums) {
            for (long termNum : ldaTermNums) {
                paramList.add(new LdaFacetParameters(topicNum, termNum));
            }
        }
        return paramList;
    }

    @Override
    public List<ModelParameters> getClusteringSettings() {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (long topicNum : ldaTopicNums) {
            paramList.add(new LdaClusterParameters(topicNum));
        }
        return paramList;
    }

    public static class LdaFacetParameters extends ModelParameters {

        long topicNum;
        long termNum;

        public LdaFacetParameters(long topicNum, long termNum) {
            this.topicNum = topicNum;
            this.termNum = termNum;
            this.paramArray = packParamsAsArray(topicNum, termNum);
        }

        public LdaFacetParameters(String paramsString) {
            this(Long.parseLong(splitParameters(paramsString)[0]),
                    Long.parseLong(splitParameters(paramsString)[1]));
        }

    }

    public static class LdaClusterParameters extends ModelParameters {

        long topicNum;

        public LdaClusterParameters(long topicNum) {
            this.topicNum = topicNum;
            this.paramArray = packParamsAsArray(topicNum);
        }

        public LdaClusterParameters(String paramsString) {
            this(Long.parseLong(paramsString));
        }

    }

    @Override
    public List<ModelParameters> getTuningSettings(List<Long> tuneMetricIndices) {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (long idx : tuneMetricIndices) {
            paramList.add(new LdaTuneParameters(idx));
        }
        return paramList;
    }

    public static class LdaTuneParameters extends ModelParameters {

        long metricIndex;

        public LdaTuneParameters(long metricIndex) {
            this.metricIndex = metricIndex;
            packParamsAsArray(metricIndex);
        }

        public LdaTuneParameters(String paramsString) {
            this(Long.parseLong(paramsString));
        }

    }

}
