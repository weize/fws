/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfFolderParameters;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
        return "fws tune-facet --facetModel=<plsa|lda|qd|gmi>\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        if (p.getString("facetModel").equals("gmj")) {
            handleGmj(p);
        } else {
            prepareDir(p);
            Job job = createJob(p);
            AppFunction.runTupleFlowJob(job, p, output);
        }

    }

    private void prepareDir(Parameters p) throws IOException {
        String model = p.getString("facetModel");
        String facetDir = p.getString("facetTuneDir");
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
        job.add(getSplitFoldStage(parameters));
        job.add(getSelectStage(parameters));
        job.add(getCopyRunStage(parameters));
        job.add(getWriteStage(parameters));

        job.connect("split", "eval", ConnectionAssignmentType.Each);
        job.connect("eval", "splitFold", ConnectionAssignmentType.Combined);
        job.connect("splitFold", "select", ConnectionAssignmentType.Each);
        job.connect("select", "copyRun", ConnectionAssignmentType.Each);
        job.connect("select", "write", ConnectionAssignmentType.Combined);

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

    private Stage getSplitFoldStage(Parameters parameter) {
        Stage stage = new Stage("splitFold");

        stage.addInput("folderParams2", new TfFolderParameters.IdOptionParametersOrder());
        stage.addOutput("folderMetrics", new TfFolderParameters.IdOptionParametersOrder());

        stage.add(new InputStep("folderParams2"));
        stage.add(new Step(SplitFoldMetrics.class, parameter));
        stage.add(Utility.getSorter(new TfFolderParameters.IdOptionParametersOrder()));
        stage.add(new OutputStep("folderMetrics"));

        return stage;
    }

    private Stage getSelectStage(Parameters parameter) {
        Stage stage = new Stage("select");

        stage.addInput("folderMetrics", new TfFolderParameters.IdOptionParametersOrder());
        stage.addOutput("folderBestParams", new TfFolderParameters.IdOptionParametersOrder());

        stage.add(new InputStep("folderMetrics"));
        stage.add(new Step(SelectBestParam.class, parameter));
        stage.add(Utility.getSorter(new TfFolderParameters.IdOptionParametersOrder()));
        stage.add(new OutputStep("folderBestParams"));

        return stage;
    }

    private Stage getCopyRunStage(Parameters parameters) {
        Stage stage = new Stage("copyRun");

        stage.addInput("folderBestParams", new TfFolderParameters.IdOptionParametersOrder());

        stage.add(new InputStep("folderBestParams"));
        stage.add(new Step(CopyRun.class, parameters));
        return stage;
    }

    private Stage getWriteStage(Parameters parameters) {
        Stage stage = new Stage("write");

        stage.addInput("folderBestParams", new TfFolderParameters.IdOptionParametersOrder());

        stage.add(new InputStep("folderBestParams"));
        stage.add(Utility.getSorter(new TfFolderParameters.OptionIdParametersOrder()));
        stage.add(new Step(WriteParams.class, parameters));
        return stage;
    }

    /**
     * Do not require tuning. Directly create a symlink to facet dir in
     * facet-run dir
     *
     * @param p
     */
    private void handleGmj(Parameters p) throws IOException {
        String model = "gmj";
        File facetRunDir = new File(Utility.getFileName(p.getString("facetRunDir"), model, "facet"));
        File facetTuneDir = new File(Utility.getFileName(p.getString("facetTuneDir"), model, "facet"));

        Utility.createDirectoryForFile(facetTuneDir);
        Path sourcePath = Paths.get(facetRunDir.toURI());
        Path targetPath = Paths.get(facetTuneDir.toURI());
        if (Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
            System.err.println("delete existing file: " + facetTuneDir.getAbsolutePath());
            Files.delete(targetPath);
        }
        Utility.info("copy " + facetRunDir.getAbsolutePath() + " to " + facetTuneDir.getAbsolutePath());
        Files.walkFileTree(sourcePath, new CopyFileVisitor(targetPath));
    }

    public static class CopyFileVisitor extends SimpleFileVisitor<Path> {

        private final Path targetPath;
        private Path sourcePath = null;

        public CopyFileVisitor(Path targetPath) {
            this.targetPath = targetPath;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir,
                final BasicFileAttributes attrs) throws IOException {
            if (sourcePath == null) {
                sourcePath = dir;
            } else {
                Files.createDirectories(targetPath.resolve(sourcePath
                        .relativize(dir)));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file,
                final BasicFileAttributes attrs) throws IOException {
            Files.copy(file,
                    targetPath.resolve(sourcePath.relativize(file)));
            return FileVisitResult.CONTINUE;
        }
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    public static class SplitFoldersForTuneEval extends StandardStep<FileName, TfFolderParameters> {

        Parameters p;

        List<ModelParameters> paramsList;
        String model;
        String facetTuneDir;
        int facetTuneRank;
        boolean skipExisting;

        public SplitFoldersForTuneEval(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
            model = p.getString("facetModel");
            paramsList = ParameterSettings.instance(p, model).getFacetingSettings();
            facetTuneRank = new Long(p.getLong("facetTuneRank")).intValue();
            facetTuneDir = Utility.getFileName(p.getString("facetTuneDir"), model, "tune");
            skipExisting = p.get("skipExisting", false);

        }

        @Override
        public void process(FileName file) throws IOException {
        }

        @Override
        public void close() throws IOException {

            long numFolders = p.getLong("cvFolderNum");

            for (int i = 1; i <= numFolders; i++) {
                String folderId = String.valueOf(i);
                for (ModelParameters params : paramsList) {
                    //String evalDir = Utility.getFileName(facetTuneDir, folderId, "eval");
                    //File evalFile = new File(Utility.getFacetEvalFileName(evalDir, model, params.toFilenameString(), facetTuneRank));
                    processor.process(new TfFolderParameters(folderId, "tune", params.toFilenameString()));
                }
            }

            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    public static class SplitFoldMetrics extends StandardStep<TfFolderParameters, TfFolderParameters> {

        long numFolders;
        List<Long> metricIndices;

        public SplitFoldMetrics(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            numFolders = p.getLong("cvFolderNum");
            metricIndices = p.getAsList("facetTuneMetricIndices", Long.class);
        }

        @Override
        public void process(TfFolderParameters fold) throws IOException {
        }

        @Override
        public void close() throws IOException {
            for (int i = 1; i <= numFolders; i++) {
                for (Long metricIdx : metricIndices) {
                    processor.process(new TfFolderParameters(String.valueOf(i), String.valueOf(metricIdx), ""));
                }
            }
            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    public static class SelectBestParam extends StandardStep<TfFolderParameters, TfFolderParameters> {

        BufferedWriter writer;
        List<ModelParameters> params;
        String tuneDir;
        String model;
        int facetTuneRank;

        public SelectBestParam(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            model = p.getString("facetModel");
            tuneDir = Utility.getFileName(p.getString("facetTuneDir"), model, "tune");
            facetTuneRank = new Long(p.getLong("facetTuneRank")).intValue();
            params = ParameterSettings.instance(p, model).getFacetingSettings();
        }

        @Override
        public void process(TfFolderParameters foldMetric) throws IOException {
            String folderId = foldMetric.id;
            int metricIndex = Integer.parseInt(foldMetric.option);

            String evalDir = Utility.getFileName(tuneDir, folderId, "eval");

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

            foldMetric.parameters = maxScoreParams.toString();
            processor.process(foldMetric);
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    public static class CopyRun implements Processor<TfFolderParameters> {

        Parameters p;
        BufferedWriter writer;
        String tuneDir;
        String model;
        String facetDir;
        String runFacetDir;

        public CopyRun(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
            model = p.getString("facetModel");
            String facetTuneModelDir = Utility.getFileName(p.getString("facetTuneDir"), model);
            tuneDir = Utility.getFileName(facetTuneModelDir, "tune");
            facetDir = Utility.getFileName(facetTuneModelDir, "facet");
            runFacetDir = Utility.getFileName(p.getString("facetRunDir"), model, "facet");

        }

        @Override
        public void process(TfFolderParameters foldParams) throws IOException {
            String folderId = foldParams.id;
            String metricIndex = foldParams.option;
            String paramsFilenameString = new ModelParameters(foldParams.parameters).toFilenameString();

            String testQueryFileName = Utility.getFileName(tuneDir, folderId, "test.query");
            TfQuery[] queries = QueryFileParser.loadQueryList(testQueryFileName);
            for (TfQuery q : queries) {
                File runFacetFile = new File(Utility.getFacetFileName(runFacetDir, q.id, model, paramsFilenameString));
                File facetFile = new File(Utility.getFacetFileName(facetDir, q.id, model, metricIndex));
                Utility.infoOpen(facetFile);
                Utility.createDirectoryForFile(facetFile);
                copy(runFacetFile, facetFile);
                Utility.infoWritten(facetFile);
            }
        }

        public static void copy(File runFacetFile, File facetFile) throws IOException {
            Utility.info("copy " + runFacetFile.getAbsoluteFile() + " to " + facetFile.getAbsoluteFile());

            Path runPath = Paths.get(runFacetFile.toURI());
            Path tunedPath = Paths.get(facetFile.toURI());
            if (Files.exists(tunedPath, LinkOption.NOFOLLOW_LINKS)) {
                System.err.println("delete existing file: " + facetFile.getAbsolutePath());
                Files.delete(tunedPath);
            }
            Files.copy(runPath, tunedPath);
        }

        @Override
        public void close() throws IOException {
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters", order = {"+option", "+id", "+parameters"})
    public static class WriteParams implements Processor<TfFolderParameters> {

        BufferedWriter writer;
        String model;
        File bestParamFile;

        public WriteParams(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            model = p.getString("facetModel");
            bestParamFile = new File(Utility.getFileName(p.getString("facetTuneDir"), model, "params"));
            writer = Utility.getWriter(bestParamFile);
        }

        @Override
        public void process(TfFolderParameters foldParams) throws IOException {
            String folderId = foldParams.id;
            String metricIndex = foldParams.option;
            ModelParameters maxScoreParams = new ModelParameters(foldParams.parameters);
            writer.write(String.format("%s\t%s\t%s\t%s\n", model, folderId,
                    metricIndex, TextProcessing.join(maxScoreParams.paramArray, "\t")));
        }

        @Override
        public void close() throws IOException {
            writer.close();
            Utility.infoWritten(bestParamFile.getAbsoluteFile());
        }
    }

}
