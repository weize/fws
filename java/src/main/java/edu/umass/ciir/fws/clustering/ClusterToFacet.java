package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.clustering.lda.LdaClusterToFacetConverter;
import edu.umass.ciir.fws.clustering.plsa.*;
import edu.umass.ciir.fws.clustering.qd.QdClusterToFacetConverter;
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
 * A generic tuple flow app for converting clusters to facets
 *
 *
 * @author wkong
 */
public class ClusterToFacet extends ProcessQueryParametersApp {

    @Override
    public String getName() {
        return "cluster-to-facet";
    }
    
    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateFacetParameters.class;
    }

    @Override
    protected Class getProcessClass(Parameters p) {
        String model = p.getString("facetModel");
        switch (model) {
            case "qd":
                return QdClusterToFacetConverter.class;
            case "plsa":
                return PlsaClusterToFacetConverter.class;
            case "lda":
                return LdaClusterToFacetConverter.class;
        }

        return null;
    }

    

    /**
     * generate parameters
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GenerateFacetParameters extends StandardStep<TfQuery, TfQueryParameters> {

        List<ModelParameters> paramsList;
        boolean skipExisting;
        String facetDir;
        String model;

        public GenerateFacetParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            model = p.getString("facetModel");
            String allFacetDir = p.getString("facetDir");
            facetDir = Utility.getFileName(allFacetDir, model, "run", "cluster");
            skipExisting = p.get("skipExisting", false);
            paramsList = ParameterSettings.instance(p, model).getFacetingSettings();

        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (ModelParameters params : paramsList) {
                File facetFile = new File(Utility.getFacetFileName(facetDir, query.id, model, params.toFilenameString()));
                if (skipExisting && facetFile.exists()) {
                    Utility.infoSkipExisting(facetFile);
                } else {
                    processor.process(new TfQueryParameters(query.id, query.text, params.toString()));
                }
            }

        }
    }
}
