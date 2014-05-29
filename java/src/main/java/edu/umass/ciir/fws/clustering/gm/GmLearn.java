/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.clustering.gm.lr.LinearRegressionModel;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfFolder;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
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
        String gmDir = p.getString("gmDir");
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

            String evalDir = Utility.getFileName(folderDir, "eval");
            Utility.createDirectory(evalDir);

            String folderPredictDir = Utility.getFileName(folderDir, "tune");
            Utility.createDirectory(folderPredictDir);

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
        job.add(getSplitQueriesForTuningStage(parameters));
        job.add(getPredictForTuningStage(parameters));
        job.add(getGmiTuneStage(parameters));
        job.add(getSplitFoldersForTuneEvalStage(parameters));
        job.add(getTuneEvalStage(parameters));

        job.connect("splitQueries", "prepareEachQueryData", ConnectionAssignmentType.Each);
        job.connect("prepareEachQueryData", "splitFolders", ConnectionAssignmentType.Combined);
        job.connect("splitFolders", "train", ConnectionAssignmentType.Each);
        job.connect("train", "splitQueriesAfterTrain", ConnectionAssignmentType.Combined);
        job.connect("splitQueriesAfterTrain", "predict", ConnectionAssignmentType.Each);
        job.connect("predict", "splitQueriesForTuning", ConnectionAssignmentType.Combined);
        job.connect("splitQueriesForTuning", "predictForTuning", ConnectionAssignmentType.Each);
        job.connect("predictForTuning", "gmiTune", ConnectionAssignmentType.Each);
        job.connect("gmiTune", "splitFoldersForTuneEval", ConnectionAssignmentType.Combined);
        job.connect("splitFoldersForTuneEval", "tuneEval", ConnectionAssignmentType.Each);

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

    private Stage getPrepareEachQueryDataStage(Parameters parameters) {
        Stage stage = new Stage("prepareEachQueryData");

        stage.addInput("queries", new TfQuery.IdOrder());
        stage.addOutput("queries2", new TfQuery.IdOrder());

        stage.add(new InputStep("queries"));
        //stage.add(new Step(TermFeatureToData.class, parameters));
        //stage.add(new Step(ExtractTermPairDataForPostiveTerm.class, parameters));
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

        stage.addInput("folders", new TfFolder.IdOrder());
        stage.addOutput("folders2", new TfFolder.IdOrder());

        stage.add(new InputStep("folders"));
        //stage.add(new Step(CollectTermTrainData.class, parameters));
        //stage.add(new Step(TrainTermModel.class, parameters));
        //stage.add(new Step(CollectPairTrainData.class, parameters));
        //stage.add(new Step(TrainPairModel.class, parameters));
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
        stage.addOutput("trainDirQueries2", new TfQueryParameters.IdParametersOrder());
        //stage.addOutput("trainDirQueries", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("trainDirQueries"));
        //stage.add(new Step(TermPredictor.class, parameters));
        //stage.add(new Step(ExtractTermPairDataForPrediectedTerms.class, parameters));
        //stage.add(new Step(PairPredictor.class, parameters));
        //stage.add(new Step(GmjClusterItems.class, parameters));
        //stage.add(new Step(GmjClusterToFacetConverter.class, parameters));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("trainDirQueries2"));
        return stage;
    }

    private Stage getSplitQueriesForTuningStage(Parameters parameters) {
        Stage stage = new Stage("splitQueriesForTuning");

        stage.addInput("trainDirQueries2", new TfQueryParameters.IdParametersOrder());
        stage.addOutput("tuneDirQueries", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("trainDirQueries2"));
        stage.add(new Step(SplitQueriesForTuning.class, parameters));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("tuneDirQueries"));
        return stage;
    }

    private Stage getPredictForTuningStage(Parameters parameters) {
        Stage stage = new Stage("predictForTuning");

        stage.addInput("tuneDirQueries", new TfQueryParameters.IdParametersOrder());
        stage.addOutput("tuneDirQueries2", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("tuneDirQueries"));
        //stage.add(new Step(TermPredictor.class, parameters));
        //stage.add(new Step(ExtractTermPairDataForPrediectedTerms.class, parameters));
        //stage.add(new Step(PairPredictor.class, parameters));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("tuneDirQueries2"));
        return stage;
    }

    private Stage getGmiTuneStage(Parameters parameters) {
        Stage stage = new Stage("gmiTune");

        stage.addInput("tuneDirQueries2", new TfQueryParameters.IdParametersOrder());
        stage.addOutput("tuneDirQueries3", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("tuneDirQueries2"));
        //stage.add(new Step(GmiClusterItems.class, parameters));
        //stage.add(new Step(GmiClusterToFacetConverter.class, parameters));
        //stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("tuneDirQueries3"));

        //stage.add(new Step(DoNonethingForQueryParams.class));
        return stage;

    }

    private Stage getSplitFoldersForTuneEvalStage(Parameters parameters) {
        Stage stage = new Stage("splitFoldersForTuneEval");

        stage.addInput("tuneDirQueries3", new TfQueryParameters.IdParametersOrder());
        stage.addOutput("foldersForTuneEval", new TfFolder.IdOrder());

        stage.add(new InputStep("tuneDirQueries3"));
        stage.add(new Step(SplitFoldersForTuneEval.class, parameters));
        stage.add(Utility.getSorter(new TfFolder.IdOrder()));
        stage.add(new OutputStep("foldersForTuneEval"));

        return stage;
    }

    private Stage getTuneEvalStage(Parameters parameters) {
        Stage stage = new Stage("tuneEval");

        stage.addInput("foldersForTuneEval", new TfFolder.IdOrder());
        //stage.addOutput("foldersForTuneEval", new TfFolder.IdOrder());

        stage.add(new InputStep("foldersForTuneEval"));
        stage.add(new Step(EvalTuneGmi.class, parameters));
        //stage.add(Utility.getSorter(new TfFolder.IdOrder()));
        //stage.add(new OutputStep("foldersForTuneEval"));

        stage.add(new Step(DoNonethingForFolder.class));
        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SplitQueriesAfterTrain extends StandardStep<TfFolder, TfQueryParameters> {

        String trainDir;

        public SplitQueriesAfterTrain(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            String gmDir = p.getString("gmDir");
            trainDir = Utility.getFileName(gmDir, "train");

        }

        @Override
        public void process(TfFolder folder) throws IOException {
            String folderDir = Utility.getFileName(trainDir, folder.id);
            String testQuery = Utility.getFileName(folderDir, "test.query");
            TfQuery[] queries = QueryFileParser.loadQueryList(testQuery);
            for (TfQuery query : queries) {
                // folder id into parameters
                String params = Utility.parametersToString(folder.id, "predict");
                processor.process(new TfQueryParameters(query.id, query.text, params));
            }
        }
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
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    public static class SplitFoldersForTuneEval extends StandardStep<TfQueryParameters, TfFolder> {

        long numFolders;
        List<Double> termProbThs;
        List<Double> pairProbThs;

        public SplitFoldersForTuneEval(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            numFolders = parameters.getJSON().getLong("cvFolderNum");
            termProbThs = p.getAsList("gmiTermProbThesholds", Double.class);
            pairProbThs = p.getAsList("gmiPairProbThesholds", Double.class);
        }

        @Override
        public void process(TfQueryParameters query) throws IOException {
        }

        @Override
        public void close() throws IOException {
            for (int i = 1; i <= numFolders; i++) {
                String folderId = String.valueOf(i);
                for (double termTh : termProbThs) {
                    for (double pairTh : pairProbThs) {
                        String newParams = Utility.parametersToString(folderId, "tune", termTh, pairTh);
                        processor.process(new TfFolder(newParams));
                    }
                }
            }
            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SplitQueriesForTuning extends StandardStep<TfQueryParameters, TfQueryParameters> {

        long numFolders;
        String trainDir;

        public SplitQueriesForTuning(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            String gmDir = p.getString("gmDir");
            trainDir = Utility.getFileName(gmDir, "train");
            numFolders = parameters.getJSON().getLong("cvFolderNum");
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
        }

        @Override
        public void close() throws IOException {
            for (int i = 1; i <= numFolders; i++) {
                String trainQuery = Utility.getFileName(trainDir, String.valueOf(i), "train.query");
                for (TfQuery query : QueryFileParser.loadQueryList(trainQuery)) {
                    String params = Utility.parametersToString(i, "tune");
                    processor.process(new TfQueryParameters(query.id, query.text, params));
                }
            }
            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    public static class DoNonething implements Processor<TfQuery> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void process(TfQuery object) throws IOException {
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    public static class DoNonethingForFolder implements Processor<TfFolder> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void process(TfFolder object) throws IOException {
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class DoNonethingForQueryParams implements Processor<TfQueryParameters> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void process(TfQueryParameters object) throws IOException {
        }
    }

}
