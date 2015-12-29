/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.clustering.lda.LdaClusterer;
import edu.umass.ciir.fws.clustering.plsa.PlsaClusterer;
import edu.umass.ciir.fws.clustering.qd.QueryDimensionTFClusterer;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;

/**
 * A generic tuple flow app for clustering for facets, supporting pLSA, LDA and
 * QD
 *
 * @author wkong
 */
public class ClusterForFacets extends ProcessQueryParametersApp {

    @Override
    public String getName() {
        return "cluster";
    }

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateClusterParameters.class;
    }

    @Override
    protected Class getProcessClass(Parameters p) {
        String model = p.getString("facetModel");
        switch (model) {
            case "qd":
                return QueryDimensionTFClusterer.class;
            case "plsa":
                return PlsaClusterer.class;
            case "lda":
                return LdaClusterer.class;
        }

        return null;
    }

    public static class GenerateClusterParameters extends StandardStep<TfQuery, TfQueryParameters> {

        List<ModelParameters> paramsList;
        boolean skipExisting;
        String clusterDir;

        public GenerateClusterParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            String model = p.getString("facetModel");
            String facetDir = p.getString("facetDir");
            clusterDir = Utility.getFileName(facetDir, model, "run", "cluster");
            skipExisting = p.get("skipExisting", false);
            paramsList = ParameterSettings.instance(p, model).getClusteringSettings();
        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (ModelParameters params : paramsList) {
                File clusterFile = new File(Utility.getQdClusterFileName(clusterDir, query.id, params.toFilenameString()));
                if (skipExisting && clusterFile.exists()) {
                    Utility.infoSkipExisting(clusterFile);
                } else {
                    processor.process(new TfQueryParameters(query.id, query.text, params.toString()));
                }

            }
        }
    }

}
