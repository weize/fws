package edu.umass.ciir.fws.clustering.plsa;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.plsa.PlsaParameterSettings.PlsaFacetParameters;
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
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Tupleflow application that does reads in query dimension clusters and output
 * query facets.
 *
 *
 * @author wkong
 */
public class PlsaClusterToFacet extends ProcessQueryParametersApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GeneratePlsaFacetParameters.class;
    }

    @Override
    protected Class getProcessClass(Parameters p) {
        return PlsaClusterToFacetConverter.class;
    }

    @Override
    public String getName() {
        return "facet-plsa";
    }

    /**
     * generate parameters
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GeneratePlsaFacetParameters extends StandardStep<TfQuery, TfQueryParameters> {

        PlsaParameterSettings plsaSettings;
        String facetDir;

        public GeneratePlsaFacetParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            plsaSettings = new PlsaParameterSettings(p);
            String runDir = p.getString("plsaRunDir");
            facetDir = Utility.getFileName(runDir, "facet");
        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (ModelParameters params : plsaSettings.getFacetingSettings()) {
                File facetFile = new File(Utility.getPlsaFacetFileName(facetDir, query.id, params.toFilenameString()));
                if (facetFile.exists()) {
                    Utility.infoFileExists(facetFile);
                } else {
                    processor.process(new TfQueryParameters(query.id, query.text, params.toString()));
                }
            }

        }

    }


}
