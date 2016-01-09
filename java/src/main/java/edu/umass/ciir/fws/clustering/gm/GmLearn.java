/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.clustering.gm.utility.PrfTrainTermPairModel;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.tool.app.ProcessQueryApp;
import edu.umass.ciir.fws.types.TfFolder;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.DirectoryUtility;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
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
 * learn term and pair model
 *
 * @author wkong
 */
public class GmLearn extends AppFunction {

    @Override
    public String getName() {
        return "gm-learn";
    }

    @Override
    public String getHelpString() {
        return "fws " + getName() + " [parameters...]\n"
                + AppFunction.getTupleFlowParameterString();
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        prepareGmDir(p);
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private void prepareGmDir(Parameters p) throws IOException {
        String gmDir = Utility.getFileName(p.getString("facetRunDir"), "gm");
        long folderNum = p.getLong("cvFolderNum");

        String predictDir = Utility.getFileName(gmDir, "predict");
        Utility.createDirectory(predictDir);

        // load queries of each folder
        String querySplitDir = p.getString("querySplitDir");
        ArrayList<TfQuery[]> queryFolders = new ArrayList<>();
        for (int i = 0; i < folderNum; i++) {
            String filename = Utility.getFileName(querySplitDir, "query.0" + i);
            TfQuery[] queries = QueryFileParser.loadQueryList(filename);
            queryFolders.add(queries);
        }

        // prepare for each folder in train dir
        String trainDir = Utility.getFileName(gmDir, "train");
        Utility.createDirectory(trainDir);
        for (int i = 0; i < folderNum; i++) {
            String folderDir = Utility.getFileName(trainDir, String.valueOf(i + 1));
            Utility.createDirectory(folderDir);

//            String evalDir = Utility.getFileName(folderDir, "eval");
//            Utility.createDirectory(evalDir);
//
//            String folderPredictDir = Utility.getFileName(folderDir, "tune");
//            Utility.createDirectory(folderPredictDir);
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
        System.err.println("prepared for " + gmDir);
    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitQueriesStage(parameters));
        job.add(getPrepareEachQueryDataStage(parameters));
        job.add(getSplitFoldersStage(parameters));
        job.add(getTrainStage(parameters));
        job.add(getSplitQueriesAfterTrainStage(parameters));
        job.add(getPredictStage(parameters));

        job.connect("splitQueries", "prepareEachQueryData", ConnectionAssignmentType.Each);
        job.connect("prepareEachQueryData", "splitFolders", ConnectionAssignmentType.Combined);
        job.connect("splitFolders", "train", ConnectionAssignmentType.Each);
        job.connect("train", "splitQueriesAfterTrain", ConnectionAssignmentType.Combined);
        job.connect("splitQueriesAfterTrain", "predict", ConnectionAssignmentType.Each);

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

    /**
     * term data, and pair data for train
     *
     * @param parameters
     * @return
     */
    private Stage getPrepareEachQueryDataStage(Parameters parameters) {
        Stage stage = new Stage("prepareEachQueryData");

        stage.addInput("queries", new TfQuery.IdOrder());
        stage.addOutput("queries2", new TfQuery.IdOrder());

        stage.add(new InputStep("queries"));
        stage.add(new Step(TermFeatureToData.class, parameters));
        stage.add(new Step(ExtractTermPairDataForPostiveTerm.class, parameters));
        stage.add(Utility.getSorter(new TfQuery.IdOrder()));
        stage.add(new OutputStep("queries2"));
        return stage;
    }

    private Stage getSplitFoldersStage(Parameters parameters) {
        Stage stage = new Stage("splitFolders");

        stage.addInput("queries2", new TfQuery.IdOrder());
        stage.addOutput("folders", new TfFolder.IdOrder());

        stage.add(new InputStep("queries2"));
        stage.add(new Step(SplitFolders.class, parameters));
        stage.add(Utility.getSorter(new TfFolder.IdOrder()));
        stage.add(new OutputStep("folders"));
        return stage;
    }

    private Stage getTrainStage(Parameters parameters) {
        Stage stage = new Stage("train");

        String gmTrainTarget = parameters.getString("gmTrainTarget");
        stage.addInput("folders", new TfFolder.IdOrder());
        stage.addOutput("folders2", new TfFolder.IdOrder());

        stage.add(new InputStep("folders"));
        stage.add(new Step(CollectTermTrainData.class, parameters));
        stage.add(new Step(CollectPairTrainData.class, parameters));
        if (gmTrainTarget.equals("prf")) {
            stage.add(new Step(PrfTrainTermPairModel.class, parameters));
        } else {
            stage.add(new Step(TrainTermModel.class, parameters));
            stage.add(new Step(TrainPairModel.class, parameters));
        }
        stage.add(Utility.getSorter(new TfFolder.IdOrder()));
        stage.add(new OutputStep("folders2"));

        return stage;
    }

    private Stage getSplitQueriesAfterTrainStage(Parameters parameters) {
        Stage stage = new Stage("splitQueriesAfterTrain");

        stage.addInput("folders2", new TfFolder.IdOrder());
        stage.addOutput("trainDirQueries", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("folders2"));
        stage.add(new Step(SplitQueriesAfterTrain.class, parameters));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("trainDirQueries"));
        return stage;
    }

    private Stage getPredictStage(Parameters parameters) {

        Stage stage = new Stage("predict");

        stage.addInput("trainDirQueries", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("trainDirQueries"));
        stage.add(new Step(TermPredictorForPrediction.class, parameters));
        stage.add(new Step(ExtractTermPairDataForPrediectedTermsForPrediction.class, parameters));
        stage.add(new Step(PairPredictorForPrediction.class, parameters));
        stage.add(new Step(ProcessQueryApp.DoNonethingForQueryParams.class));
        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    public static class SplitFolders extends StandardStep<TfQuery, TfFolder> {

        long numFolders;

        public SplitFolders(TupleFlowParameters parameters) throws IOException {
            numFolders = parameters.getJSON().getLong("cvFolderNum");

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
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SplitQueriesAfterTrain extends StandardStep<TfFolder, TfQueryParameters> {

        String trainDir;

        public SplitQueriesAfterTrain(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            String gmDir = Utility.getFileName(p.getString("facetRunDir"), "gm");
            trainDir = Utility.getFileName(gmDir, "train");

        }

        @Override
        public void process(TfFolder folder) throws IOException {
            String folderDir = Utility.getFileName(trainDir, folder.id);
            String testQuery = Utility.getFileName(folderDir, "test.query");
            TfQuery[] queries = QueryFileParser.loadQueryList(testQuery);
            for (TfQuery query : queries) {
                processor.process(new TfQueryParameters(query.id, query.text, folder.id));
            }
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class TermPredictorForPrediction extends TermPredictor {

        String predictDir;

        public TermPredictorForPrediction(TupleFlowParameters parameters) {
            super(parameters);
            Parameters p = parameters.getJSON();
            String gmDir = Utility.getFileName(p.getString("facetRunDir"), "gm");
            predictDir = DirectoryUtility.getGmPredictDir(gmDir);;
        }

        @Override
        public String getPredictBaseDir(String foldId) {
            return predictDir;
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class ExtractTermPairDataForPrediectedTermsForPrediction extends ExtractTermPairDataForPrediectedTerms {

        String predictDir;

        public ExtractTermPairDataForPrediectedTermsForPrediction(TupleFlowParameters parameters) throws Exception {
            super(parameters);
            Parameters p = parameters.getJSON();
            String gmDir = Utility.getFileName(p.getString("facetRunDir"), "gm");
            predictDir = Utility.getFileName(gmDir, "predict");
        }

        @Override
        public String getPredictBaseDir(String foldId) {
            return predictDir;
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class PairPredictorForPrediction extends PairPredictor {

        String predictDir;

        public PairPredictorForPrediction(TupleFlowParameters parameters) throws Exception {
            super(parameters);
            Parameters p = parameters.getJSON();
            String gmDir = Utility.getFileName(p.getString("facetRunDir"), "gm");
            predictDir = Utility.getFileName(gmDir, "predict");
        }

        @Override
        public String getPredictBaseDir(String foldId) {
            return predictDir;
        }
    }

}
