/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws2.pilot.qperformance;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.clustering.gm.TrainDataSampler;
import edu.umass.ciir.fws.clustering.gm.lr.LinearRegressionModel;
import edu.umass.ciir.fws.eval.QueryFacetEvaluator;
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

/**
 * input: qpFeature file input: eval File input: evaluation measure input:
 * classify dir input: threshold for good/bad performance
 *
 * @author wkong
 */
public class ClassifyQueryPerofmranceCV extends AppFunction {

    static String qpClassifyDir;
    static String allFeatureFile;

    @Override
    public String getName() {
        return "classify-query-performance";
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        qpClassifyDir = p.getString("qpClassifyDir");

        prepareClassifyDir(p);
        prepareTrainFile(p);

        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);
    }

    private void prepareClassifyDir(Parameters p) throws IOException {
        long folderNum = p.getLong("qpCVFolderNum");
        String predictDir = Utility.getFileName(qpClassifyDir, "predict");
        Utility.createDirectory(predictDir);

        // load queries of each folder
        String querySplitDir = p.getString("querySplitDir");
        ArrayList<TfQuery[]> queryFolders = new ArrayList<>();
        for (int i = 0; i < folderNum; i++) {
            String filename = Utility.getFileName(querySplitDir, "query.0" + i);
            TfQuery[] queries = QueryFileParser.loadQueryList(filename);
            queryFolders.add(queries);
        }

        String trainDir = Utility.getFileName(qpClassifyDir, "train");
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
    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitFoldersStage(parameters));
        job.add(getTrainStage(parameters));
        job.add(getEvalStage(parameters));

        job.connect("splitFolders", "process", ConnectionAssignmentType.Each);
        job.connect("process", "eval", ConnectionAssignmentType.Combined);

        return job;
    }

    private Stage getSplitFoldersStage(Parameters p) {
        Stage stage = new Stage("splitFolders");

        stage.addOutput("folders", new TfFolder.IdOrder());

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

    private void prepareTrainFile(Parameters p) throws IOException {
        // load performance
        String evalFilename = p.getString("qfFacetEvalFile");
        long measureIdx = p.getLong("qfMeasureIdx");
        HashMap<String, Double> qidScore = new HashMap<>(); // qid -> measure score
        BufferedReader reader = Utility.getReader(evalFilename);
        String line;

        while ((line = reader.readLine()) != null) {
            // qid score1 score2
            String[] elems = line.split("\t");
            String qid = elems[0];
            double score = Double.parseDouble(elems[(int) measureIdx]);
            qidScore.put(qid, score);
        }
        reader.close();

        // load query features
        String qfFilename = p.getString("qpFeature");
        BufferedReader qfReader = Utility.getReader(qfFilename);
        allFeatureFile = Utility.getFileName(qpClassifyDir, "train", "perf.qid.feature");
        BufferedWriter writer = Utility.getWriter(allFeatureFile);
        while ((line = qfReader.readLine()) != null) {
            // qid qf1 qf2
            String[] elems = line.split("\t");
            String qid = elems[0];
            double score = qidScore.get(qid);
            writer.write(String.format("%f\t%s\n", score, line));
        }
        qfReader.close();
        writer.close();
    }

    @Verified
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolder", order = {"+id"})
    public static class SplitFolders implements ExNihiloSource<TfFolder> {

        public Processor<TfFolder> processor;
        long numFolders;

        public SplitFolders(TupleFlowParameters parameters) throws IOException {
            numFolders = parameters.getJSON().getLong("qpCVFolderNum");

        }

        @Override
        public void run() throws IOException {
            // note that the order is numeric instead of alphabet
            for (int i = 1; i <= numFolders; i++) {
                processor.process(new TfFolder(String.valueOf(i)));
            }
            processor.close();
        }

        @Override
        public void setProcessor(org.lemurproject.galago.tupleflow.Step nextStage) throws IncompatibleProcessorException {
            Linkage.link(this, nextStage);
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
            String qpClassifyDir = p.getString("qpClassifyDir");
            trainDir = Utility.getFileName(qpClassifyDir, "train");
            allPredictFile = new File(Utility.getFileName(qpClassifyDir, "predict", "all.predict"));
            allPredictEvalFile = new File(Utility.getFileName(qpClassifyDir, "predict", "all.predict.eval"));
            folderNum = p.getLong("qpCVFolderNum");

        }

        @Override
        public void process(TfFolder object) throws IOException {

        }

        @Override
        public void close() throws IOException {
            // combine test predict files in each folders
            int posTotal = 0;
            int posReturn = 0;
            int posLabeled = 0;
            double avgPerf = 0;

            BufferedWriter writer = Utility.getWriter(allPredictFile);
            BufferedWriter writerEval = Utility.getWriter(allPredictEvalFile);
            for (int i = 1; i <= folderNum; i++) {
                String folderDir = Utility.getFileName(trainDir, "" + i);
                File trainEvalFile = new File(Utility.getFileName(folderDir, "train.qp.predcit.eval")); // for tune
                File predictFile = new File(Utility.getFileName(folderDir, "test.qp.predcit"));

                BufferedReader readerTrainEval = Utility.getReader(trainEvalFile);
                String line = readerTrainEval.readLine();
                String[] elems = line.split("\t");
                Double threshold = Double.parseDouble(elems[1]);
                readerTrainEval.close();
                BufferedReader readerPredict = Utility.getReader(predictFile);
                while ((line = readerPredict.readLine()) != null) {
                    elems = line.split("\t");
                    double perf = Double.parseDouble(elems[4]); // performance
                    double prob = Double.parseDouble(elems[0]);
                    int label = Integer.parseInt(elems[1]);
                    int predictLabel = prob >= threshold ? 1 : -1;
                    writer.write(String.format("%d\t%s\n", predictLabel, line));
                    if (label == 1) {
                        posTotal++;
                    }
                    if (predictLabel ==1) {
                        posLabeled++;
                        avgPerf += perf;
                        
                    }
                    if (predictLabel == 1 && label == 1) {
                        posReturn++;
                    }

                }
                readerPredict.close();
            }
            double precision = (double) posReturn / (double) posLabeled;
            double recall = (double) posReturn / (double) posTotal;
            double f1 = QueryFacetEvaluator.f1(precision, recall);
            avgPerf /= posLabeled;
            writer.close();
            writerEval.write(String.format("f1\t%s\nprecision\t%s\nrecall\t%s\nperfAvg\t%f\n", f1, precision, recall, avgPerf));
            writerEval.write(String.format("pos\t%d\n", posLabeled));
            writerEval.close();
            Utility.infoWritten(allPredictFile);
            Utility.infoWritten(allPredictEvalFile);
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
        TrainDataSampler dataSampler;
        double threshold;
        File trainFile;
        File testFile;
        File modelFile;
        File scalerFile;
        File predictFile;
        File predictTrainFile; // for tuning
        File predictTrainEvalFile;
        List<Long> qfIndices;

        public FolderProcess(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            String gmDir = p.getString("qpClassifyDir");
            threshold = p.getDouble("qpThreshold");
            trainDir = Utility.getFileName(gmDir, "train");
            predictDir = Utility.getFileName(gmDir, "predict");
            qfIndices = p.getAsList("queryFeatureIndices", Long.class);

            //dataSampler = new TrainDataSampler();
        }

        @Override
        public void process(TfFolder folder) throws IOException {

            folderDir = Utility.getFileName(trainDir, folder.id);
            trainQueryFile = Utility.getFileName(folderDir, "train.query"); // input
            testQueryFile = Utility.getFileName(folderDir, "test.query"); // input
            trainFile = new File(Utility.getFileName(folderDir, "train.qp.data.gz")); // output
            testFile = new File(Utility.getFileName(folderDir, "test.qp.data.gz")); // output
            modelFile = new File(Utility.getFileName(folderDir, "train.qp.model"));
            scalerFile = new File(Utility.getFileName(folderDir, "train.qp.scaler"));
            predictFile = new File(Utility.getFileName(folderDir, "test.qp.predcit"));
            predictTrainFile = new File(Utility.getFileName(folderDir, "train.qp.predcit")); // for tune
            predictTrainEvalFile = new File(Utility.getFileName(folderDir, "train.qp.predcit.eval")); // for tune

            prepareTrainTestFile();
            train();
            double threshold = tune(); // for tune f1
            predict(threshold);
            processor.process(folder);
        }

        private void prepareTrainTestFile() throws IOException {

            HashMap<String, TfQuery> trainQueryMap = QueryFileParser.loadQueryMap(new File(trainQueryFile));
            HashMap<String, TfQuery> testQueryMap = QueryFileParser.loadQueryMap(new File(testQueryFile));
            // create train feature file
            BufferedReader reader = Utility.getReader(allFeatureFile);
            String line;
            BufferedWriter writerTrain = Utility.getWriter(trainFile);
            BufferedWriter writerTest = Utility.getWriter(testFile);
            while ((line = reader.readLine()) != null) {
                String[] elems = line.split("\t");
                double score = Double.parseDouble(elems[0]);
                int label = score >= threshold ? 1 : -1;
                String qid = elems[1];
                List<String> features = Arrays.asList(elems).subList(2, elems.length);
                if (trainQueryMap.containsKey(qid)) {
                    String query = trainQueryMap.get(qid).text;
                    writerTrain.write(String.format("%d\t%s\t#%d\t%s\t%s\t%f\n", label, TextProcessing.join(features, "\t"), label, qid, query, score));
                } else {
                    String query = testQueryMap.get(qid).text;
                    writerTest.write(String.format("%d\t%s\t#%d\t%s\t%s\t%f\n", label, TextProcessing.join(features, "\t"), label, qid, query, score));
                }
            }
            reader.close();
            writerTrain.close();
            writerTest.close();
            Utility.infoWritten(trainFile);
            Utility.infoWritten(testFile);
        }

        private void train() throws IOException {
            LinearRegressionModel tModel = new LinearRegressionModel(qfIndices);
            tModel.train(trainFile, modelFile, scalerFile);
            Utility.infoWritten(modelFile);
            Utility.infoWritten(scalerFile);
        }

        private double tune() throws IOException {
            // predict on train data
            LinearRegressionModel model = new LinearRegressionModel(qfIndices);
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
                double f1 = QueryFacetEvaluator.f1(precision, recall);
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

        private void predict(double threshold) throws IOException {
            LinearRegressionModel model = new LinearRegressionModel(qfIndices);
            model.predict(testFile, modelFile, scalerFile, predictFile);
            Utility.infoWritten(predictFile);
        }

    }

}
