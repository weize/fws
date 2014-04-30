package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.types.QueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.IOException;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Tupleflow application that does query dimension clustering.
 *
 *
 * @author wkong
 */
public class QdClusterCandidateLists extends ProcessQueryParametersApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateQdClusterParameters.class;
    }

    @Override
    protected Class getProcessClass() {
        return QueryDimensionClusterers.class;
    }

    @Override
    protected String AppName() {
        return "cluster-qd";
    }

    /**
     *
     * @author wkong
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.Query")
    @OutputClass(className = "edu.umass.ciir.fws.types.QueryParameters")
    public static class GenerateQdClusterParameters extends StandardStep<Query, QueryParameters> {

        List<Double> distanceMaxs;
        List<Double> websiteCountMins;

        public GenerateQdClusterParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            distanceMaxs = p.getList("qdDistanceMaxs");
            websiteCountMins = p.getList("qdWebsiteCountMins");

        }

        @Override
        public void process(Query query) throws IOException {
            for (double distanceMax : distanceMaxs) {
                for (double websiteCountMin : websiteCountMins) {
                    String parameters = Utility.parametersToString(distanceMax, websiteCountMin);
                    processor.process(new QueryParameters(query.id, query.text, parameters));
                }
            }

        }

    }

}