package edu.umass.ciir.fws.clustering.plsa;

import edu.umass.ciir.fws.clustering.ModelParameters;
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
 * Tupleflow application that does plsa clustering
 *
 *
 * @author wkong
 */
public class PlsaClusterItems extends ProcessQueryParametersApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GeneratePlsaClusterParameters.class;
    }

    @Override
    protected Class getProcessClass(Parameters p) {
        return PlsaClusterer.class;
    }

    @Override
    public String getName() {
        return "cluster-plsa";
    }

    /**
     *
     * @author wkong
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GeneratePlsaClusterParameters extends StandardStep<TfQuery, TfQueryParameters> {

        PlsaParameterSettings plsaSettings;
        String clusterDir;

        public GeneratePlsaClusterParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            plsaSettings = new PlsaParameterSettings(p);
            String runDir = p.getString("plsaRunDir");
            clusterDir = Utility.getFileName(runDir, "cluster");
        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (ModelParameters params : plsaSettings.getClusteringSettings()) {
                File clusterFile = new File(Utility.getPlsaClusterFileName(clusterDir, query.id, params.toFilenameString()));
                if (clusterFile.exists()) {
                    Utility.infoFileExists(clusterFile);
                } else {
                    processor.process(new TfQueryParameters(query.id, query.text, params.toString()));
                }
            }

        }

    }
}
