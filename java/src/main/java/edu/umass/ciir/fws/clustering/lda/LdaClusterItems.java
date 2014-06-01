/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.lda;

import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
public class LdaClusterItems extends ProcessQueryParametersApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateLdaClusterParameters.class;
    }

    @Override
    protected Class getProcessClass() {
        return LdaClusterer.class;
    }

    @Override
    public String getName() {
        return "cluster-lda";
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GenerateLdaClusterParameters extends StandardStep<TfQuery, TfQueryParameters> {

        List<Long> topicNums;
        String clusterDir;

        public GenerateLdaClusterParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            topicNums = p.getList("ldaTopicNums");
            String runDir = p.getString("ldaRunDir");
            clusterDir = Utility.getFileName(runDir, "cluster");
        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (long topicNum : topicNums) {
                File clusterFile = new File(Utility.getLdaClusterFileName(clusterDir, query.id, topicNum));
                if (clusterFile.exists()) {
                    Utility.infoFileExists(clusterFile);
                } else {
                    String parameters = Utility.parametersToString(topicNum);
                    processor.process(new TfQueryParameters(query.id, query.text, parameters));
                }
            }

        }

    }
}
