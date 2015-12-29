package edu.umass.ciir.fws.clustering.lda;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.lda.LdaParameterSettings.LdaFacetParameters;
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
 * Tupleflow application that reads in lda clusters and selects top N terms in
 * each cluster, and outputs query facets.
 *
 *
 * @author wkong
 */
public class LdaClusterToFacet extends ProcessQueryParametersApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateLdaFacetParameters.class;
    }

    @Override
    protected Class getProcessClass(Parameters p) {
        return LdaClusterToFacetConverter.class;
    }

    @Override
    public String getName() {
        return "facet-lda";
    }

    /**
     * generate parameters
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GenerateLdaFacetParameters extends StandardStep<TfQuery, TfQueryParameters> {

        LdaParameterSettings ldaSettings;
        String facetDir;

        public GenerateLdaFacetParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            ldaSettings = new LdaParameterSettings(p);
            String runDir = p.getString("ldaRunDir");
            facetDir = Utility.getFileName(runDir, "facet");
        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (ModelParameters params : ldaSettings.getFacetingSettings()) {
                File facetFile = new File(Utility.getLdaFacetFileName(facetDir, query.id, params.toFilenameString()));
                if (facetFile.exists()) {
                    Utility.infoFileExists(facetFile);
                } else {
                    processor.process(new TfQueryParameters(query.id, query.text, params.toString()));
                }

            }

        }

    }

}
