package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.ParameterSettings;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
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
        return QueryDimensionTFClusterer.class;
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

        QdParameterSettings qdParams;
        String clusterDir;

        public GenerateQdClusterParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            qdParams = new QdParameterSettings(p);
            String runDir = p.getString("qdRunDir");
            clusterDir = Utility.getFileName(runDir, "cluster");

        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (ModelParameters params : qdParams.getClusterParametersList()) {
                File clusterFile = new File(Utility.getQdClusterFileName(clusterDir, query.id, params.toFilenameString()));
                if (clusterFile.exists()) {
                    Utility.infoFileExists(clusterFile);
                } else {
                    processor.process(new TfQueryParameters(query.id, query.text, params.toString()));
                }
            }
        }

    }

}
