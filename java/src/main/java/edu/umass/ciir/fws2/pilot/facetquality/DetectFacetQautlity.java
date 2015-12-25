/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws2.pilot.facetquality;

import edu.umass.ciir.fws2.pilot.qperformance.*;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.clustering.gm.TrainDataSampler;
import edu.umass.ciir.fws.clustering.gm.lr.LinearRegressionModel;
import edu.umass.ciir.fws.eval.CombinedEvaluator;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfFolder;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.ExNihiloSource;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.IncompatibleProcessorException;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Linkage;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 *
 * @author wkong
 */
public class DetectFacetQautlity extends AppFunction {

    static String facetQaulityDir;
    static String allFeatureFile;

    @Override
    public String getName() {
        return "detect-facet-qaulity";
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        facetQaulityDir = p.getString("fqDir");

        prepareDir(p);
        //prepareTrainFile(p);

        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);
    }

    private void prepareDir(Parameters p) throws IOException {
        long folderNum = p.getLong("fqCVFolderNum");
        String predictDir = Utility.getFileName(facetQaulityDir, "predict");
        Utility.createDirectory(predictDir);

        // load queries of each folder
        String querySplitDir = p.getString("querySplitDir");
        ArrayList<TfQuery[]> queryFolders = new ArrayList<>();
        for (int i = 0; i < folderNum; i++) {
            String filename = Utility.getFileName(querySplitDir, "query.0" + i);
            TfQuery[] queries = QueryFileParser.loadQueryList(filename);
            queryFolders.add(queries);
        }

        String trainDir = Utility.getFileName(facetQaulityDir, "train");
        Utility.createDirectory(trainDir);
        for (int i = 0; i < folderNum; i++) {
            String folderDir = Utility.getFileName(trainDir, String.valueOf(i + 1));
            Utility.createDirectory(folderDir);
            // train.qp.data
            // train.qp.model
            // train.qp.scaler

            // test query
            String testQuery = Utility.getFileName(folderDir, "test.query");
            QueryFileParser.output(queryFolders.get(i), testQuery);
            // train query
            String trainQuery = Utility.getFileName(folderDir, "train.query");
            ArrayList<TfQuery> trainQueries = new ArrayList<>();
            for (int j = 0; j < folderNum; j++) {
                if (j != i) {
                    trainQueries.addAll(Arrays.asList(queryFolders.get(j)));
                }
            }
            QueryFileParser.output(trainQueries.toArray(new TfQuery[0]), trainQuery);
        }

        String facetDir = Utility.getFileName(facetQaulityDir, "facet");
        Utility.createDirectory(facetDir);
    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitQueriesStage(parameters));
        job.add(getPrepareTestFilesStage(parameters));
        job.add(getSplitFoldersStage(parameters));
        job.add(getTrainStage(parameters));
        job.add(getEvalStage(parameters));

        job.connect("splitQueries", "prepareTestFiles", ConnectionAssignmentType.Each);
        job.connect("prepareTestFiles", "splitFolders", ConnectionAssignmentType.Combined);
        job.connect("splitFolders", "process", ConnectionAssignmentType.Each);
        job.connect("process", "eval", ConnectionAssignmentType.Combined);

        return job;
    }

    private Stage getSplitQueriesStage(Parameters parameter) {
        Stage stage = new Stage("splitQueries");

        stage.addOutput("queries", new TfQuery.IdOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(Utility.getSorter(new FileName.FilenameOrder()));
        stage.add(new Step(QueryFileParser.class));
        stage.add(Utility.getSorter(new TfQuery.IdOrder()));
        stage.add(new OutputStep("queries"));

        return stage;
    }

    private Stage getPrepareTestFilesStage(Parameters p) {
        Stage stage = new Stage("prepareTestFiles");

        stage.addInput("queries", new TfQuery.IdOrder());
        stage.addOutput("queries2", new TfQuery.IdOrder());

        stage.add(new InputStep("queries"));
        stage.add(new Step(PrepareTestFiles.class, p));
        stage.add(new OutputStep("queries2"));

        return stage;
    }

    private Stage getSplitFoldersStage(Parameters p) {
        Stage stage = new Stage("splitFolders");

        stage.addInput("queries2", new TfQuery.IdOrder());
        stage.addOutput("folders", new TfFolder.IdOrder());

        stage.add(new InputStep("queries2"));
        stage.add(new Step(SplitFolders.class, p));
        stage.add(new OutputStep("folders"));

        return stage;
    }

    private Stage getTrainStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("folders", new TfFolder.IdOrder());
        stage.addOutput("folders2", new TfFolder.IdOrder());

        stage.add(new InputStep("folders"));
        stage.add(new Step(FolderProcess.class, parameters));
        stage.add(new OutputStep("folders2"));

        return stage;
    }

    private Stage getEvalStage(Parameters parameters) {
        Stage stage = new Stage("eval");

        stage.addInput("folders2", new TfFolder.IdOrder());

        stage.add(new InputStep("folders2"));
        stage.add(new Step(Eval.class, parameters));

        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery", order = {"+id"})
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQuery", order = {"+id"})
    public static class PrepareTestFiles extends StandardStep<TfQuery, TfQuery> {

        String predictDir;
        double threshold;
        int fqMeasureIdx;
        String fqDir;

        public PrepareTestFiles(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            fqDir = p.getString("fqDir");
            predictDir = Utility.getFileName(fqDir, "predict");
            fqMeasureIdx = (int) p.getLong("fqMeasureIdx"); // index according to qid.f.quality.feature (from FacetFeatures)
            threshold = p.getDouble("fqThreshold");
        }

        @Override
        public void process(TfQuery q) throws IOException {
            File featureFile = new File(Utility.getFileNameWithSuffix(fqDir, "feature", q.id, "f.quality.feature"));
            File testFile = new File(Utility.getFileNameWithSuffix(predictDir, q.id, q.id, "f.data.gz"));
            Utility.createDirectoryForFile(testFile);
            BufferedReader reader = Utility.getReader(featureFile);
            BufferedWriter writer = Utility.getWriter(testFile);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] elems = line.split("\t");
                double score = Double.parseDouble(elems[fqMeasureIdx]);
                List<String> features = Arrays.asList(elems).subList(FacetFeatures.fIdxStart, FacetFeatures.size);
                String itemList = elems[elems.length - 1];
                int label = score >= threshold ? 1 : -1;
                writer.write(String.format("%d\t%s\t#%d\t%s\t%f\n", label, TextProcessing.join(features, "\t"), label, itemList, score));
            }
            reader.close();
            writer.close();
            Utility.infoWritten(testFile);
            processor.process(q);
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery", order = {"+id"})
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolder", order = {"+id"})
    public static class SplitFolders extends StandardStep<TfQuery, TfFolder> {

        long numFolders;

        public SplitFolders(TupleFlowParameters parameters) throws IOException {
            numFolders = parameters.getJSON().getLong("fqCVFolderNum");

        }

        @Override
        public void process(TfQuery query) throws IOException {
        }

        @Override
        public void close() throws IOException {
            for (int i = 1; i <= numFolders; i++) {
                processor.process(new TfFolder(String.valueOf(i)));
            }
            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolder", order = {"+id"})
    public static class Eval implements Processor<TfFolder> {

        long folderNum;
        String trainDir;
        File allPredictFile;
        File allPredictEvalFile;

        public Eval(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            String fqDir = p.getString("fqDir");
            trainDir = Utility.getFileName(fqDir, "train");
            allPredictFile = new File(Utility.getFileName(fqDir, "predict", "all.predict"));
            allPredictEvalFile = new File(Utility.getFileName(fqDir, "predict", "all.predict.eval"));
            folderNum = p.getLong("fqCVFolderNum");

        }

        @Override
        public void process(TfFolder object) throws IOException {

        }

        @Override
        public void close() throws IOException {
            // combine test predict files in each folders
//            int posTotal = 0;
//            int posReturn = 0;
//            int posLabeled = 0;
//            double avgPerf = 0;
//
//            BufferedWriter writer = Utility.getWriter(allPredictFile);
//            BufferedWriter writerEval = Utility.getWriter(allPredictEvalFile);
//            for (int i = 1; i <= folderNum; i++) {
//                String folderDir = Utility.getFileName(trainDir, "" + i);
//                File trainEvalFile = new File(Utility.getFileName(folderDir, "train.qp.predcit.eval")); // for tune
//                File predictFile = new File(Utility.getFileName(folderDir, "test.qp.predcit"));
//
//                BufferedReader readerTrainEval = Utility.getReader(trainEvalFile);
//                String line = readerTrainEval.readLine();
//                String[] elems = line.split("\t");
//                Double threshold = Double.parseDouble(elems[1]);
//                readerTrainEval.close();
//                BufferedReader readerPredict = Utility.getReader(predictFile);
//                while ((line = readerPredict.readLine()) != null) {
//                    elems = line.split("\t");
//                    double perf = Double.parseDouble(elems[4]); // performance
//                    double prob = Double.parseDouble(elems[0]);
//                    int label = Integer.parseInt(elems[1]);
//                    int predictLabel = prob >= threshold ? 1 : -1;
//                    writer.write(String.format("%d\t%s\n", predictLabel, line));
//                    if (label == 1) {
//                        posTotal++;
//                    }
//                    if (predictLabel == 1) {
//                        posLabeled++;
//                        avgPerf += perf;
//
//                    }
//                    if (predictLabel == 1 && label == 1) {
//                        posReturn++;
//                    }
//
//                }
//                readerPredict.close();
//            }
//            double precision = (double) posReturn / (double) posLabeled;
//            double recall = (double) posReturn / (double) posTotal;
//            double f1 = CombinedEvaluator.f1(precision, recall);
//            avgPerf /= posLabeled;
//            writer.close();
//            writerEval.write(String.format("f1\t%s\nprecision\t%s\nrecall\t%s\nperfAvg\t%f\n", f1, precision, recall, avgPerf));
//            writerEval.write(String.format("pos\t%d\n", posLabeled));
//            writerEval.close();
//            Utility.infoWritten(allPredictFile);
//            Utility.infoWritten(allPredictEvalFile);
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolder", order = {"+id"})
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolder", order = {"+id"})
    public static class FolderProcess extends StandardStep<TfFolder, TfFolder> {

        String predictDir;
        String trainDir;
        String folderDir;
        String trainQueryFile;
        String testQueryFile;
        String facetDir;
        double threshold;
        double presentThreshold;
        File trainFile;
        File modelFile;
        File scalerFile;

        File predictTrainFile; // for tuning
        File predictTrainEvalFile;
        List<Long> fqIndices;

        int fqMeasureIdx;
        String fqDir;

        public FolderProcess(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            fqDir = p.getString("fqDir");
            trainDir = Utility.getFileName(fqDir, "train");
            predictDir = Utility.getFileName(fqDir, "predict");
            String allFacetDir = p.getString("facetDir");
            facetDir = Utility.getFileName(allFacetDir, "rerank", "facet");

            fqIndices = p.getAsList("facetFeatureIndices", Long.class);

            fqMeasureIdx = (int) p.getLong("fqMeasureIdx");
            threshold = p.getDouble("fqThreshold");
            presentThreshold = p.getDouble("fqPresentThreshold");

            //dataSampler = new TrainDataSampler();
        }

        @Override
        public void process(TfFolder folder) throws IOException {

            folderDir = Utility.getFileName(trainDir, folder.id);
            trainQueryFile = Utility.getFileName(folderDir, "train.query"); // input
            testQueryFile = Utility.getFileName(folderDir, "test.query"); // input
            trainFile = new File(Utility.getFileName(folderDir, "train.f.data.gz")); // output

            ///testFile = new File(Utility.getFileName(folderDir, "test.qp.data.gz")); // output
            modelFile = new File(Utility.getFileName(folderDir, "train.f.model"));
            scalerFile = new File(Utility.getFileName(folderDir, "train.f.scaler"));

//            predictTrainFile = new File(Utility.getFileName(folderDir, "train.qp.predcit")); // for tune
//            predictTrainEvalFile = new File(Utility.getFileName(folderDir, "train.qp.predcit.eval")); // for tune
            //prepareTestFileForEachQuery();
            prepareTrainFileForEachFolder();
            train();
            //double threshold = tune(); // for tune f1
            predict();
            rerank();
            processor.process(folder);
        }

        private void prepareTrainFileForEachFolder() throws IOException {
            // combine testFile to trainFile
            TfQuery[] trainQueries = QueryFileParser.loadQueryList(trainQueryFile);
            ArrayList<File> dataFiles = new ArrayList<>();
            for (TfQuery q : trainQueries) {
                File testFile = new File(Utility.getFileNameWithSuffix(predictDir, q.id, q.id, "f.data.gz"));
                dataFiles.add(testFile);
            }
            TrainDataSampler.combine(dataFiles, trainFile);
            Utility.infoWritten(trainFile);
        }

        private void train() throws IOException {
            LinearRegressionModel model = new LinearRegressionModel(fqIndices);
            model.train(trainFile, modelFile, scalerFile);
            Utility.infoWritten(modelFile);
            Utility.infoWritten(scalerFile);
        }

        private double tune() throws IOException {
            // predict on train data
            LinearRegressionModel model = new LinearRegressionModel(fqIndices);
            model.predict(trainFile, modelFile, scalerFile, predictTrainFile);
            Utility.infoWritten(predictTrainFile);

            // tuen F1
            BufferedReader reader = Utility.getReader(predictTrainFile);
            String line;
            ArrayList<ScoredItem> results = new ArrayList<>(); // hack: using item as label, and score as prob
            int posTotal = 0;
            while ((line = reader.readLine()) != null) {
                String[] elems = line.split("\t");
                double prob = Double.parseDouble(elems[0]);
                String label = elems[1];
                if (label.equals("1")) {
                    posTotal++;
                }
                results.add(new ScoredItem(label, prob));
            }

            Collections.sort(results);
            int posReturn = 0;
            int sizeReturn = 0;
            double f1Best = Double.NEGATIVE_INFINITY;
            double precisionBest = 0;
            double recallBest = 0;
            double threshold = 0;
            for (ScoredItem result : results) {
                sizeReturn++;
                if (result.item.equals("1")) {
                    posReturn++;
                }
                double precision = (double) posReturn / (double) sizeReturn;
                double recall = (double) posReturn / (double) posTotal;
                double f1 = CombinedEvaluator.f1(precision, recall);
                if (f1 > f1Best) {
                    f1Best = f1;
                    precisionBest = precision;
                    recallBest = recall;
                    threshold = result.score;
                }
            }

            BufferedWriter writer = Utility.getWriter(predictTrainEvalFile);
            writer.write(String.format("threshold\t%s\nf1\t%s\nprecision\t%s\nrecall\t%s\n", threshold, f1Best, precisionBest, recallBest));
            writer.close();
            return threshold;

        }

        private void predict() throws IOException {
            TfQuery[] testQueries = QueryFileParser.loadQueryList(testQueryFile);
            for (TfQuery q : testQueries) {
                File testFile = new File(Utility.getFileNameWithSuffix(predictDir, q.id, q.id, "f.data.gz"));
                File predictFile = new File(Utility.getFileNameWithSuffix(predictDir, q.id, q.id, "f.predict"));
                LinearRegressionModel model = new LinearRegressionModel(fqIndices);
                model.predict(testFile, modelFile, scalerFile, predictFile);
                Utility.infoWritten(predictFile);
            }
        }

        private void rerank() throws IOException {
            TfQuery[] testQueries = QueryFileParser.loadQueryList(testQueryFile);
            for (TfQuery q : testQueries) {
                File predictFile = new File(Utility.getFileNameWithSuffix(predictDir, q.id, q.id, "f.predict"));
                File facetFile = new File(Utility.getFileNameWithSuffix(facetDir, q.id, q.id, "rerank.facet"));
                Utility.createDirectoryForFile(facetFile);
                BufferedReader reader = Utility.getReader(predictFile);
                String line;
                ArrayList<ScoredFacet> facets = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    String[] elems = line.split("\t");
                    double score = Double.parseDouble(elems[0]);
                    if (score < presentThreshold) {
                        continue;
                    }
                    double quality = Double.parseDouble(elems[3]);
                    String itemList = elems[2];

                    ScoredFacet facet = new ScoredFacet(score);
                    //ScoredFacet facet = new ScoredFacet(quality); // oracle: rank by quality
                    for (String t : itemList.split("\\|")) {
                        facet.addItem(new ScoredItem(t, 0));
                    }
                    facets.add(facet);
                }
                Collections.sort(facets);
                ScoredFacet.outputAsFacets(facets, facetFile);
                Utility.infoWritten(facetFile);
            }
        }

    }

}
