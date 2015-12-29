/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.clustering.gm.GmLearn;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfFolder;
import edu.umass.ciir.fws.types.TfFolderParameters;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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
 *
 * @author wkong
 */
public class TuneFacetModel extends AppFunction {

    @Override
    public String getName() {
        return "tune-facet";
    }

    @Override
    public String getHelpString() {
        return "fws tune-facet --facetModel=<plsa|lda|qd>\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        prepareDir(p);
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private void prepareDir(Parameters p) throws IOException {
        String model = p.getString("facetModel");
        String facetDir = p.getString("facetDir");
        long folderNum = p.getLong("cvFolderNum");

        String querySplitDir = p.getString("querySplitDir");
        ArrayList<TfQuery[]> queryFolders = new ArrayList<>();
        for (int i = 0; i < folderNum; i++) {
            String filename = Utility.getFileName(querySplitDir, "query.0" + i);
            TfQuery[] queries = QueryFileParser.loadQueryList(filename);
            queryFolders.add(queries);
        }

        // prepare for each folder in train dir
        String tuneDir = Utility.getFileName(facetDir, model, "tune");
        Utility.createDirectory(tuneDir);
        for (int i = 0; i < folderNum; i++) {
            String folderDir = Utility.getFileName(tuneDir, String.valueOf(i + 1));
            Utility.createDirectory(folderDir);

            String evalDir = Utility.getFileName(folderDir, "eval");
            Utility.createDirectory(evalDir);

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
        System.err.println("prepared for " + tuneDir);

    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getEvalStage(parameters));
        job.add(getSelectStage(parameters));
        job.add(getCopyRunStage(parameters));

        job.connect("split", "eval", ConnectionAssignmentType.Each);
        job.connect("eval", "select", ConnectionAssignmentType.Combined);
        job.connect("select", "copyRun", ConnectionAssignmentType.Each);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("folderParams", new TfFolderParameters.IdOptionParametersOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(SplitFoldersForTuneEval.class, parameter));
        stage.add(Utility.getSorter(new TfFolderParameters.IdOptionParametersOrder()));
        stage.add(new OutputStep("folderParams"));

        return stage;
    }

    private Stage getEvalStage(Parameters parameter) {
        Stage stage = new Stage("eval");

        stage.addInput("folderParams", new TfFolderParameters.IdOptionParametersOrder());
        stage.addOutput("folderParams2", new TfFolderParameters.IdOptionParametersOrder());

        stage.add(new InputStep("folderParams"));
        stage.add(new Step(EvalFacetModelForTuning.class, parameter));
        stage.add(Utility.getSorter(new TfFolderParameters.IdOptionParametersOrder()));
        stage.add(new OutputStep("folderParams2"));

        return stage;
    }

    private Stage getSelectStage(Parameters parameter) {
        Stage stage = new Stage("select");

        stage.addInput("folderParams2", new TfFolderParameters.IdOptionParametersOrder());
        stage.addOutput("queryParams", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("folderParams2"));
        stage.add(new Step(SelectBestParam.class, parameter));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("queryParams"));

        return stage;
    }

    private Stage getCopyRunStage(Parameters parameters) {
        Stage stage = new Stage("copyRun");

        stage.addInput("queryParams", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("queryParams"));
        stage.add(new Step(CopyRun.class, parameters));
        stage.add(new Step(GmLearn.DoNonethingForQueryParams.class));
        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class CopyRun extends StandardStep<TfQueryParameters, TfQueryParameters> {

        Parameters p;
        BufferedWriter writer;
        String modelDir;
        String model;
        String facetDir;
        String runFacetDir;

        public CopyRun(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
            model = p.getString("facetModel");
            modelDir = Utility.getFileName(p.getString("facetDir"), model);
            runFacetDir = Utility.getFileName(modelDir, "run", "facet");
            facetDir = Utility.getFileName(modelDir, "facet");
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            String metricIndex = queryParams.text; // user query text field for metric index
            String paramsFilenameString = queryParams.parameters;

            File runFacetFile = new File(Utility.getFacetFileName(runFacetDir, queryParams.id, model, paramsFilenameString));
            File facetFile = new File(Utility.getFacetFileName(facetDir, queryParams.id, model, metricIndex));
            Utility.infoOpen(facetFile);
            Utility.createDirectoryForFile(facetFile);
            makesLink(runFacetFile, facetFile);
            Utility.infoWritten(facetFile);

        }

        private void makesLink(File runFacetFile, File facetFile) throws IOException {
            System.err.println(runFacetFile.getAbsoluteFile());
            System.err.println(facetFile.getAbsoluteFile());
            if (facetFile.exists()) {
                System.err.println("delete existing link");
                facetFile.delete();
            }
            Files.createSymbolicLink(Paths.get(facetFile.toURI()), Paths.get(runFacetFile.toURI()));
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SelectBestParam extends StandardStep<TfFolderParameters, TfQueryParameters> {

        Parameters p;
        BufferedWriter writer;
        String modelDir;
        String model;
        int facetTuneRank;

        public SelectBestParam(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
            model = p.getString("facetModel");
            modelDir = Utility.getFileName(p.getString("facetDir"), model);
            facetTuneRank = new Long(p.getLong("facetTuneRank")).intValue();
        }

        @Override
        public void process(TfFolderParameters object) throws IOException {

        }

        @Override
        public void close() throws IOException {
            long numFolders = p.getLong("cvFolderNum");
            List<Long> metricIndices = p.getAsList("facetTuneMetricIndices", Long.class);
            File bestParamFile = new File(Utility.getFileName(modelDir, "params"));
            writer = Utility.getWriter(bestParamFile);

            for (Long metricIdx : metricIndices) {
                for (int i = 1; i <= numFolders; i++) {
                    findBestParamAndEmitRun(String.valueOf(i), metricIdx.intValue());
                }
            }

            processor.close();
            writer.close();
            Utility.infoWritten(bestParamFile);
        }

        private void findBestParamAndEmitRun(String folderId, int metricIndex) throws IOException {

            String folderDir = Utility.getFileName(modelDir, "tune", folderId);
            String evalDir = Utility.getFileName(folderDir, "eval");

            List<ModelParameters> params = ParameterSettings.instance(p, model).getFacetParametersList();

            double maxScore = Double.NEGATIVE_INFINITY;
            ModelParameters maxScoreParams = null;
            for (ModelParameters param : params) {
                File evalFile = new File(Utility.getFacetEvalFileName(evalDir, model, param.toFilenameString(), facetTuneRank));
                double score = QueryMetrics.getAvgScore(evalFile, metricIndex);
                if (score > maxScore) {
                    maxScore = score;
                    maxScoreParams = param;
                }
            }

            writer.write(String.format("%s\t%s\t%d\t%s\n", model, folderId,
                    metricIndex, TextProcessing.join(maxScoreParams.paramArray, "\t")));

            String testQueryFileName = Utility.getFileName(folderDir, "test.query");
            TfQuery[] queries = QueryFileParser.loadQueryList(testQueryFileName);
            for (TfQuery q : queries) {
                // user query text field for metric index
                processor.process(new TfQueryParameters(q.id, String.valueOf(metricIndex), maxScoreParams.toFilenameString()));
            }

        }

    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    public static class SplitFoldersForTuneEval extends StandardStep<FileName, TfFolderParameters> {

        Parameters p;

        public SplitFoldersForTuneEval(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
        }

        @Override
        public void process(FileName file) throws IOException {
        }

        @Override
        public void close() throws IOException {
            String model = p.getString("facetModel");
            long numFolders = p.getLong("cvFolderNum");

            for (int i = 1; i <= numFolders; i++) {
                String folderId = String.valueOf(i);
                ParameterSettings settings = ParameterSettings.instance(p, model);
                for (ModelParameters params : settings.getFacetParametersList()) {
                    processor.process(new TfFolderParameters(folderId, "tune", params.toFilenameString()));
                }
            }
            
            processor.close();
        }

    }
}
