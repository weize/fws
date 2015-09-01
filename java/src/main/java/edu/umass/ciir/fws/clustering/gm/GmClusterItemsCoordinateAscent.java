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
        classes.add(GmCoordinateAscentClusterItems.class);
        classes.add(GmCoordinateAscentClusterToFacetConverter.class);
        classes.add(DoNonething.class);
        return classes;
    }

     /**
     *
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GmCoordinateAscentClusterToFacetConverter extends StandardStep<TfQueryParameters, TfQueryParameters> {

        String facetDir;
        String clusterDir;

        public GmCoordinateAscentClusterToFacetConverter(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            facetDir = p.getString("gmFacetDir");
            clusterDir = p.getString("gmClusterDir");
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            Utility.infoProcessingQuery(queryParams.id);
            
            String qid = queryParams.id;
            
            // loadClusters clusters
            File clusterFile = new File(Utility.getGmcaClusterFileName(clusterDir, queryParams.id));
            List<ScoredFacet> clusters = ScoredFacet.loadClusters(clusterFile);

            File facetFile = new File(Utility.getGmcaFacetFileName(facetDir, qid));
            Utility.createDirectoryForFile(facetFile);
            Utility.infoOpen(facetFile);
            ScoredFacet.outputAsFacets(clusters, facetFile);
            Utility.infoWritten(facetFile);
            processor.process(queryParams);
        }
    }
    
  
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GmCoordinateAscentClusterItems extends StandardStep<TfQueryParameters, TfQueryParameters> {

        String clusterDir = "gmClusterDir";

        public GmCoordinateAscentClusterItems(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            clusterDir = p.getString("gmClusterDir");
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            File termPredictFile = new File(Utility.getGmTermPredictFileName(clusterDir, queryParams.id));
            File termPairPredictFile = new File(Utility.getGmTermPairPredictFileName(clusterDir, queryParams.id));
            
            Utility.infoProcessingQuery(queryParams.id);
            
            File clusterFile = new File(Utility.getGmcaClusterFileName(clusterDir, queryParams.id));
            Utility.createDirectoryForFile(clusterFile);

            GmCoordinateAscentClusterer gmca = new GmCoordinateAscentClusterer();
            List<ScoredFacet> clusters = gmca.cluster(termPredictFile, termPairPredictFile);
            ScoredFacet.output(clusters, clusterFile);
            Utility.infoWritten(clusterFile);
            processor.process(queryParams);
        }
    }
    
  
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GenerateGmClusterParameters extends StandardStep<TfQuery, TfQueryParameters> {

        String tFeatureIndices = "1:2:3:4:23:24:25:26:27:28:29:30:31:32:33";
        String pFeatureIndices = "1:2:3:4";
        double termProbTh = 0.6;
        double pairProbTh = 0.5;

        public GenerateGmClusterParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
        }

        @Override
        public void process(TfQuery query) throws IOException {
            String parameters = edu.umass.ciir.fws.utility.Utility.parametersToString(tFeatureIndices, pFeatureIndices, termProbTh, pairProbTh);
            processor.process(new TfQueryParameters(query.id, query.text, parameters));
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
