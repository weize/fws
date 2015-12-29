package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.clustering.qd.QdParameterSettings.QdFacetParameters;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
public class QdClusterToFacet extends ProcessQueryParametersApp {

    @Override
    protected Class getProcessClass(Parameters p) {
        return QdClusterToFacetConverter.class;
    }

    @Override
    public String getName() {
        return "facet-qd";
    }

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateQdFacetParameters.class;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GenerateQdFacetParameters extends StandardStep<TfQuery, TfQueryParameters> {

        String facetDir;
        QdParameterSettings qdSettings;

        public GenerateQdFacetParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            qdSettings = new QdParameterSettings(p);
            String runDir = p.getString("qdRunDir");
            facetDir = Utility.getFileName(runDir, "facet");

        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (ModelParameters params : qdSettings.getFacetingSettings()) {
                File facetFile = new File(Utility.getQdFacetFileName(facetDir, query.id, params.toFilenameString()));
                if (facetFile.exists()) {
                    Utility.infoFileExists(facetFile);
                } else {
                    processor.process(new TfQueryParameters(query.id, query.text, params.toString()));
                }
            }
        }

    }


}
