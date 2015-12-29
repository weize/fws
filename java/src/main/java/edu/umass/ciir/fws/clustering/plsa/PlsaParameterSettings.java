/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.plsa;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.ParameterSettings;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class PlsaParameterSettings extends ParameterSettings {

    List<Long> topicNums;
    List<Long> termNums;

    public PlsaParameterSettings(Parameters p) {
        topicNums = p.getList("plsaTopicNums");
        termNums = p.getList("plsaTermNums");
    }

    @Override
    public List<ModelParameters> getFacetingSettings() {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (long topicNum : topicNums) {
            for (long termNum : termNums) {
                paramList.add(new PlsaFacetParameters(topicNum, termNum));
            }
        }
        return paramList;
    }

    @Override
    public List<ModelParameters> getClusteringSettings() {
        ArrayList<ModelParameters> paramList = new ArrayList<>();
        for (long topicNum : topicNums) {
            paramList.add(new PlsaClusterParameters(topicNum));
        }
        return paramList;
    }

    public static class PlsaFacetParameters extends ModelParameters {

        long topicNum;
        long termNum;

        public PlsaFacetParameters(long topicNum, long termNum) {
            this.topicNum = topicNum;
            this.termNum = termNum;
            this.paramArray = packParamsAsArray(topicNum, termNum);
        }

        public PlsaFacetParameters(String paramsString) {
            this(Long.parseLong(splitParameters(paramsString)[0]),
                    Long.parseLong(splitParameters(paramsString)[1]));
        }

    }

    public static class PlsaClusterParameters extends ModelParameters {

        long topicNum;

        public PlsaClusterParameters(long topicNum) {
            this.topicNum = topicNum;
            this.paramArray = packParamsAsArray(topicNum);
        }

        public PlsaClusterParameters(String paramsString) {
            this(Long.parseLong(paramsString));
        }

    }

}
