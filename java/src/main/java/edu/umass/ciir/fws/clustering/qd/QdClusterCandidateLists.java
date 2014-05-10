package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
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
    public String getName() {
        return "cluster-qd";
    }

    /**
     *
     * @author wkong
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GenerateQdClusterParameters extends StandardStep<TfQuery, TfQueryParameters> {

        List<Double> distanceMaxs;
        List<Double> websiteCountMins;

        public GenerateQdClusterParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            distanceMaxs = p.getList("qdDistanceMaxs");
            websiteCountMins = p.getList("qdWebsiteCountMins");

        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (double distanceMax : distanceMaxs) {
                for (double websiteCountMin : websiteCountMins) {
                    String parameters = Utility.parametersToString(distanceMax, websiteCountMin);
                    processor.process(new TfQueryParameters(query.id, query.text, parameters));
                }
            }

        }

    }

}
