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
public class GmClusterItems extends ProcessQueryParametersMultiStepApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateGmClusterParameters.class;
    }

    @Override
    public String getName() {
        return "cluster-gm";
    }

    @Override
    protected List<Class> getProcessClasses() {
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(FeatureToData.class);
        classes.add(TermPredictor.class);
        classes.add(TermPairDataExtractor.class);
        classes.add(TermPairPredictor.class);
        classes.add(GmiClusterItems.class);
        classes.add(GmjClusterItems.class);
        classes.add(GmiClusterToFacetConverter.class);
        classes.add(GmjClusterToFacetConverter.class);
        classes.add(DoNonething.class);
        return classes;
    }

     /**
     *
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GmjClusterToFacetConverter extends StandardStep<TfQueryParameters, TfQueryParameters> {

        String facetDir;
        String clusterDir;

        public GmjClusterToFacetConverter(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            facetDir = p.getString("gmFacetDir");
            clusterDir = p.getString("gmClusterDir");
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            Utility.infoProcessingQuery(queryParams.id);
            
            String qid = queryParams.id;
            
            // loadClusters clusters
            File clusterFile = new File(Utility.getGmjClusterFileName(clusterDir, queryParams.id));
            List<ScoredFacet> clusters = ScoredFacet.loadClusters(clusterFile);

            File facetFile = new File(Utility.getGmjFacetFileName(facetDir, qid));
            Utility.createDirectoryForFile(facetFile);
            ScoredFacet.outputAsFacets(clusters, facetFile);
            Utility.infoWritten(facetFile);
            processor.process(queryParams);
        }
    }
    
    
    /**
     *
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GmiClusterToFacetConverter extends StandardStep<TfQueryParameters, TfQueryParameters> {

        String facetDir;
        String clusterDir;

        public GmiClusterToFacetConverter(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            facetDir = p.getString("gmFacetDir");
            clusterDir = p.getString("gmClusterDir");
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            Utility.infoProcessingQuery(queryParams.id);
            
            String qid = queryParams.id;
            String[] params = Utility.splitParameters(queryParams.parameters);
            double termProbTh = Double.parseDouble(params[2]);
            double pairProbTh = Double.parseDouble(params[3]);
            
            // loadClusters clusters
            File clusterFile = new File(Utility.getGmiClusterFileName(clusterDir, queryParams.id, termProbTh, pairProbTh));
            List<ScoredFacet> clusters = ScoredFacet.loadClusters(clusterFile);

            File facetFile = new File(Utility.getGmiFacetFileName(facetDir, qid, termProbTh, pairProbTh));
            Utility.createDirectoryForFile(facetFile);
            ScoredFacet.outputAsFacets(clusters, facetFile);
            Utility.infoWritten(facetFile);
            processor.process(queryParams);
        }
    }
    
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GmjClusterItems extends StandardStep<TfQueryParameters, TfQueryParameters> {

        String clusterDir = "gmClusterDir";

        public GmjClusterItems(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            clusterDir = p.getString("gmClusterDir");
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            File termPredictFile = new File(Utility.getGmTermPredictFileName(clusterDir, queryParams.id));
            File termPairPredictFile = new File(Utility.getGmTermPairPredictFileName(clusterDir, queryParams.id));
            
            Utility.infoProcessingQuery(queryParams.id);
            
            File clusterFile = new File(Utility.getGmjClusterFileName(clusterDir, queryParams.id));
            Utility.createDirectoryForFile(clusterFile);

            GmJointClusterer gmj = new GmJointClusterer();
            List<ScoredFacet> clusters = gmj.cluster(termPredictFile, termPairPredictFile);
            ScoredFacet.output(clusters, clusterFile);
            Utility.infoWritten(clusterFile);
            processor.process(queryParams);
        }
    }
    
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GmiClusterItems extends StandardStep<TfQueryParameters, TfQueryParameters> {

        String clusterDir = "gmClusterDir";

        public GmiClusterItems(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            clusterDir = p.getString("gmClusterDir");
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            File termPredictFile = new File(Utility.getGmTermPredictFileName(clusterDir, queryParams.id));
            File termPairPredictFile = new File(Utility.getGmTermPairPredictFileName(clusterDir, queryParams.id));
            String[] params = Utility.splitParameters(queryParams.parameters);

            Utility.infoProcessingQuery(queryParams.id);
            double termProbTh = Double.parseDouble(params[2]);
            double pairProbTh = Double.parseDouble(params[3]);
            File clusterFile = new File(Utility.getGmiClusterFileName(clusterDir, queryParams.id, termProbTh, pairProbTh));
            Utility.createDirectoryForFile(clusterFile);

            GmIndependentClusterer gmi = new GmIndependentClusterer(termProbTh, pairProbTh);
            List<ScoredFacet> clusters = gmi.cluster(termPredictFile, termPairPredictFile);
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
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class TermPredictor extends StandardStep<TfQueryParameters, TfQueryParameters> {

        String clusterDir = "gmClusterDir";
        File modelFile;
        File scalerFile;

        public TermPredictor(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            clusterDir = p.getString("gmClusterDir");
            modelFile = new File(p.getString("gmTermModel"));
            scalerFile = new File(p.getString("gmTermScaler"));
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            File dataFile = new File(Utility.getGmTermDataFileName(clusterDir, queryParams.id));
            File predictFile = new File(Utility.getGmTermPredictFileName(clusterDir, queryParams.id));
            Utility.infoProcessing(dataFile);
            Utility.createDirectoryForFile(predictFile);
            String[] params = Utility.splitParameters(queryParams.parameters);
            String[] tFeatureIndices = params[0].split(":");
            int[] selectedFeatureIndices = new int[tFeatureIndices.length];
            for (int i = 0; i < tFeatureIndices.length; i++) {
                selectedFeatureIndices[i] = Integer.parseInt(tFeatureIndices[i]);
            }
            LinearRegressionModel model = new LinearRegressionModel(selectedFeatureIndices);
            model.predict(dataFile, modelFile, scalerFile, predictFile);
            Utility.infoWritten(predictFile);
            
            processor.process(queryParams);
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class TermPairPredictor extends StandardStep<TfQueryParameters, TfQueryParameters> {

        String clusterDir = "gmClusterDir";
        File modelFile;
        File scalerFile;

        public TermPairPredictor(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            clusterDir = p.getString("gmClusterDir");
            modelFile = new File(p.getString("gmTermPairModel"));
            scalerFile = new File(p.getString("gmTermPairScaler"));
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            File dataFile = new File(Utility.getGmTermPairDataFileName(clusterDir, queryParams.id));
            File predictFile = new File(Utility.getGmTermPairPredictFileName(clusterDir, queryParams.id));
            Utility.infoProcessing(dataFile);
            Utility.createDirectoryForFile(predictFile);
            String[] params = Utility.splitParameters(queryParams.parameters);
            String[] pFeatureIndices = params[1].split(":");
            int[] selectedFeatureIndices = new int[pFeatureIndices.length];
            for (int i = 0; i < pFeatureIndices.length; i++) {
                selectedFeatureIndices[i] = Integer.parseInt(pFeatureIndices[i]);
            }
            LinearRegressionModel model = new LinearRegressionModel(selectedFeatureIndices);
            model.predict(dataFile, modelFile, scalerFile, predictFile);
            Utility.infoWritten(predictFile);
            processor.process(queryParams);
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class FeatureToData extends StandardStep<TfQueryParameters, TfQueryParameters> {

        String featureDir = "featureDir";
        String clusterDir = "gmClusterDir";

        public FeatureToData(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            featureDir = p.getString("featureDir");
            clusterDir = p.getString("gmClusterDir");
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {

            // input file
            File featureFile = new File(Utility.getTermFeatureFileName(featureDir, queryParams.id));
            // output file
            File dataFile = new File(Utility.getGmTermDataFileName(clusterDir, queryParams.id));

            System.err.println("processing " + featureFile.getAbsolutePath());
            Utility.createDirectoryForFile(dataFile);
            BufferedReader reader = Utility.getReader(featureFile);
            BufferedWriter writer = Utility.getWriter(dataFile);
            String line;
            while ((line = reader.readLine()) != null) {
                // format: term<tab>f1<tab>f2<tab>...
                String[] fields = line.split("\t");
                String term = fields[0];
                int label = -1;
                String data = label + "\t" + TextProcessing.join(Arrays.asList(fields).subList(1, fields.length), "\t");
                String comment = String.format("%d\t%s\t%s", label, queryParams.id, term);
                writer.write(data + "\t#" + comment);
                writer.newLine();

            }
            writer.close();
            reader.close();
            Utility.infoWritten(dataFile);

            processor.process(queryParams);
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
