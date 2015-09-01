/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.gm.lr.LinearRegressionModel;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersMultiStepApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * This is just for the initial run.
 *
 * @author wkong
 */
public class GmClusterItemsCoordinateAscent extends ProcessQueryParametersMultiStepApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateGmClusterParameters.class;
    }

    @Override
    public String getName() {
        return "cluster-gm-ca";
    }

    @Override
    protected List<Class> getProcessClasses() {
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(GmcClusterItems.class);
        classes.add(AppendFacetRankerParameter.class);
        classes.add(GmcClusterToFacetConverter.class);
        classes.add(DoNonething.class);
        return classes;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GenerateGmClusterParameters extends StandardStep<TfQuery, TfQueryParameters> {

        public GenerateGmClusterParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
        }

        @Override
        public void process(TfQuery query) throws IOException {
            processor.process(new TfQueryParameters(query.id, query.text, ""));
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class DoNonething implements Processor<TfQueryParameters> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void process(TfQueryParameters o) throws IOException {
        }
    }

}
