/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.srank;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.clustering.gm.GmLearn;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
import edu.umass.ciir.fws.ffeedback.RunOracleCandidateExpasions;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfFolder;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.query.StructuredQuery;
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
 * Tune pseudo relevance model
 *
 * @author wkong
 */
public class TunePRM extends AppFunction {

    @Override
    public String getName() {
        return "tune-prm";
    }

    @Override
    public String getHelpString() {
        return "fws tune-prm config.json\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        prepareDir(p);
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    public static class PrmDirectory {

        String headDir;
        String tuneDir;
        String rankDir;
        private File bestParamFile;

        public PrmDirectory(Parameters p) {
            headDir = Utility.getFileName(p.getString("srankDir"), "prm");
            tuneDir = Utility.getFileName(headDir, "tune");
            rankDir = Utility.getFileName(headDir, "rankDir");
            bestParamFile = new File(Utility.getFileName(tuneDir, "params"));
        }

        public String getTuneCurFolderDir(String folderId) {
            return Utility.getFileName(tuneDir, folderId);
        }

        public File getTuneCurTrainQueryFile(String folderId) {
            return new File(Utility.getFileName(getTuneCurFolderDir(folderId), "train.query"));
        }

        private File getRankFile(String folderId, double fbOrigWt, long fbDoc, long fbTerm) {
            String name = String.format("prm.%s.rank", Utility.parametersToFileNameString(fbOrigWt, fbDoc, fbTerm));
            return new File(Utility.getFileName(getTuneCurFolderDir(folderId), "rank", name));
        }

        private File getEvalFile(String folderId, double fbOrigWt, long fbDoc, long fbTerm) {
            String name = String.format("prm.%s.eval", Utility.parametersToFileNameString(fbOrigWt, fbDoc, fbTerm));
            return new File(Utility.getFileName(getTuneCurFolderDir(folderId), "eval", name));
        }

        private File getTevalFile(String folderId, double fbOrigWt, long fbDoc, long fbTerm) {
            String name = String.format("prm.%s.teval", Utility.parametersToFileNameString(fbOrigWt, fbDoc, fbTerm));
            return new File(Utility.getFileName(getTuneCurFolderDir(folderId), "eval", name));
        }

        private File getTuneCurTestQueryFile(String folderId) {
            return new File(Utility.getFileName(getTuneCurFolderDir(folderId), "test.query"));
        }

        private File getTestRankFile(String qid) {
            return new File(Utility.getFileName(headDir, "rank", String.format("%s.prm.rank", qid)));
        }

    }

    private void prepareDir(Parameters p) throws IOException {
        PrmDirectory prmDir = new PrmDirectory(p);
        long folderNum = p.getLong("cvFolderNum");

        String querySplitDir = p.getString("querySplitDir");
        ArrayList<TfQuery[]> queryFolders = new ArrayList<>();
        for (int i = 0; i < folderNum; i++) {
            String filename = Utility.getFileName(querySplitDir, "query.0" + i);
            TfQuery[] queries = QueryFileParser.loadQueryList(filename);
            queryFolders.add(queries);
        }

        Utility.createDirectory(prmDir.headDir);
        // prepare for each folder in train dir
        String tuneDir = prmDir.tuneDir;
        Utility.createDirectory(tuneDir);
        Utility.createDirectory(prmDir.rankDir);
        for (int i = 0; i < folderNum; i++) {
            String folderDir = Utility.getFileName(tuneDir, String.valueOf(i + 1));
            Utility.createDirectory(folderDir);

            String evalDir = Utility.getFileName(folderDir, "eval");
            Utility.createDirectory(evalDir);

            Utility.createDirectory(Utility.getFileName(folderDir, "rank"));
            Utility.createDirectory(Utility.getFileName(folderDir, "param"));

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
        job.add(getProcessStage(parameters));
        job.add(getSelectStage(parameters));
        job.add(getRunStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);
        job.connect("process", "select", ConnectionAssignmentType.Combined);
        job.connect("select", "run", ConnectionAssignmentType.Each);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("folderParams", new TfFolder.IdOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(SplitFoldersForTuneEval.class, parameter));
        stage.add(Utility.getSorter(new TfFolder.IdOrder()));
        stage.add(new OutputStep("folderParams"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameter) {
        Stage stage = new Stage("process");

        stage.addInput("folderParams", new TfFolder.IdOrder());
        stage.addOutput("folderParams2", new TfFolder.IdOrder());

        stage.add(new InputStep("folderParams"));
        stage.add(new Step(RunQueryForTuning.class, parameter));
        stage.add(new Step(RunEvalForTuning.class, parameter));
        stage.add(Utility.getSorter(new TfFolder.IdOrder()));
        stage.add(new OutputStep("folderParams2"));

        return stage;
    }

    private Stage getSelectStage(Parameters parameter) {
        Stage stage = new Stage("select");

        stage.addInput("folderParams2", new TfFolder.IdOrder());
        stage.addOutput("queryParams", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("folderParams2"));
        stage.add(new Step(SelectBestParam.class, parameter));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("queryParams"));

        return stage;
    }

    private Stage getRunStage(Parameters parameters) {
        Stage stage = new Stage("run");

        stage.addInput("queryParams", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("queryParams"));
        stage.add(new Step(Run.class, parameters));
        stage.add(new Step(GmLearn.DoNonethingForQueryParams.class));
        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class Run extends StandardStep<TfQueryParameters, TfQueryParameters> {

        Retrieval retrieval;
        Parameters p;
        PrmDirectory prmDir;
        BufferedWriter writer;

        public Run(TupleFlowParameters parameters) throws Exception {
            p = parameters.getJSON();
            prmDir = new PrmDirectory(p);
            retrieval = RetrievalFactory.instance(p);
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            Utility.infoProcessing(queryParams);
            String[] params = Utility.splitParameters(queryParams.parameters);
            String folderId = params[0];
            String metricIndex = params[1];
            double fbOrigWt = Double.parseDouble(params[2]);
            long fbDoc = Long.parseLong(params[3]);
            long fbTerm = Long.parseLong(params[4]);

            File rankFile = prmDir.getTestRankFile(queryParams.id);
            Utility.infoOpen(rankFile);
            BufferedWriter writer = Utility.getWriter(rankFile);

            String queryNumber = queryParams.id;
            String queryText = RunQueryForTuning.getPrmQuery(queryParams.text, fbOrigWt, fbDoc, fbTerm);

            // parse and transform query into runnable form
            List<ScoredDocument> results = null;

            Node root = StructuredQuery.parse(queryText);
            Node transformed;
            try {
                System.err.println("run " + queryParams.id + " " + queryText);
                transformed = retrieval.transformQuery(root, p);
                // run query
                results = retrieval.executeQuery(transformed, p).scoredDocuments;
            } catch (Exception ex) {
                Logger.getLogger(RunOracleCandidateExpasions.class.getName()).log(Level.SEVERE, "error in running for "
                        + queryParams.toString(), ex);
                throw new IOException();
            }

            // if we have some results -- print in to output stream
            if (!results.isEmpty()) {
                for (ScoredDocument sd : results) {
                    writer.write(sd.toTRECformat(queryNumber));
                    writer.newLine();
                }
            } else {
                writer.write(String.format("%s Q0 clueweb09-xxxxxx-xx-xxxxx 1 -1 galago", queryNumber));
                writer.newLine();
            }

            writer.close();
            Utility.infoWritten(rankFile);

            processor.process(queryParams);
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SelectBestParam extends StandardStep<TfFolder, TfQueryParameters> {

        Parameters p;
        PrmDirectory prmDir;
        PrmParameterGenerator paramGen;
        BufferedWriter writer;
        final static int metricIndex = 0; // map

        public SelectBestParam(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
        }

        @Override
        public void process(TfFolder object) throws IOException {

        }

        @Override
        public void close() throws IOException {
            long numFolders = p.getLong("cvFolderNum");
            paramGen = new PrmParameterGenerator(p);
            prmDir = new PrmDirectory(p);

            File bestParamFile = prmDir.bestParamFile;
            Utility.infoOpen(bestParamFile);
            writer = Utility.getWriter(bestParamFile);

            for (int i = 1; i <= numFolders; i++) {
                findBestParamAndEmitRun(String.valueOf(i));
            }

            processor.close();
            writer.close();
            Utility.infoWritten(bestParamFile);
        }

        private void findBestParamAndEmitRun(String folderId) throws IOException {
            List<String> prmParams = paramGen.getAllParams();

            double maxScore = Double.NEGATIVE_INFINITY;
            String maxScoreParams = "";
            for (String prmParam : prmParams) {
                String[] params = Utility.splitParameters(prmParam);
                double fbOrigWt = Double.parseDouble(params[0]);
                long fbDoc = Long.parseLong(params[1]);
                long fbTerm = Long.parseLong(params[2]);

                File evalFile = prmDir.getEvalFile(folderId, fbOrigWt, fbDoc, fbTerm);
                double score = QueryMetrics.getAvgScore(evalFile, metricIndex);
                if (score > maxScore) {
                    maxScore = score;
                    maxScoreParams = prmParam;
                }
            }

            writer.write(String.format("%s\t%s\t%d\t%s\n", "prm", folderId,
                    metricIndex, TextProcessing.join(Utility.splitParameters(maxScoreParams), "\t")));

            File testQueryFile = prmDir.getTuneCurTestQueryFile(folderId);
            TfQuery[] queries = QueryFileParser.loadQueryList(testQueryFile);
            for (TfQuery q : queries) {
                String qParams = Utility.parametersToString(folderId, metricIndex, maxScoreParams);
                processor.process(new TfQueryParameters(q.id, q.text, qParams));
            }
        }

    }

    public static class PrmParameterGenerator {

        Parameters p;
        //* #rm:fbOrigWt=0.5:fbDocs=10:fbTerms=10( query )
        List<Long> fbDocs;
        List<Long> fbTerms;
        List<Double> fbOrigWts;

        public PrmParameterGenerator(Parameters p) {
            fbDocs = p.getAsList("prmFbDocs");
            fbTerms = p.getAsList("prmFbTerms");
            fbOrigWts = p.getAsList("prmfbOrigWts");
        }

        public List<String> getAllParams() {
            ArrayList<String> params = new ArrayList<>();
            for (Double fbOrigWt : fbOrigWts) {
                for (long docNum : fbDocs) {
                    for (long termNum : fbTerms) {
                        params.add(Utility.parametersToString(fbOrigWt, docNum, termNum));
                    }
                }
            }
            return params;
        }
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    public static class SplitFoldersForTuneEval extends StandardStep<FileName, TfFolder> {

        Parameters p;
        PrmParameterGenerator paramGen;

        public SplitFoldersForTuneEval(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
            paramGen = new PrmParameterGenerator(p);
        }

        @Override
        public void process(FileName file) throws IOException {
        }

        @Override
        public void close() throws IOException {
            long numFolders = p.getLong("cvFolderNum");
            List<String> prmParams = paramGen.getAllParams();

            for (int i = 1; i <= numFolders; i++) {
                String folderId = String.valueOf(i);
                for (String prmParam : prmParams) {
                    String newParams = Utility.parametersToString(folderId, prmParam);
                    processor.process(new TfFolder(newParams));
                }
            }
            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    public static class RunQueryForTuning extends StandardStep<TfFolder, TfFolder> {

        Retrieval retrieval;
        Parameters p;
        PrmDirectory prmDir;
        PrmParameterGenerator paramGen;

        public RunQueryForTuning(TupleFlowParameters parameters) throws Exception {
            p = parameters.getJSON();
            paramGen = new PrmParameterGenerator(p);
            prmDir = new PrmDirectory(p);
            retrieval = RetrievalFactory.instance(p);
        }

        @Override
        public void process(TfFolder folder) throws IOException {
            Utility.infoProcessing(folder);
            String[] params = Utility.splitParameters(folder.id);
            String folderId = params[0];
            double fbOrigWt = Double.parseDouble(params[1]);
            long fbDoc = Long.parseLong(params[2]);
            long fbTerm = Long.parseLong(params[3]);

            File trainQueryFile = prmDir.getTuneCurTrainQueryFile(folderId);
            File rankFile = prmDir.getRankFile(folderId, fbOrigWt, fbDoc, fbTerm);

            if (rankFile.exists()) {
                Utility.infoFileExists(rankFile);
                processor.close();
                return;
            }

            Utility.infoOpen(rankFile);
            BufferedWriter writer = Utility.getWriter(rankFile);
            List<TfQuery> queries = QueryFileParser.loadQueries(trainQueryFile);
            for (TfQuery q : queries) {

                String queryNumber = q.id;
                String queryText = getPrmQuery(q.text, fbOrigWt, fbDoc, fbTerm);

                // parse and transform query into runnable form
                List<ScoredDocument> results = null;

                Node root = StructuredQuery.parse(queryText);
                Node transformed;
                try {
                    System.err.println("run " + q.id + " " + queryText);
                    transformed = retrieval.transformQuery(root, p);
                    // run query
                    results = retrieval.executeQuery(transformed, p).scoredDocuments;
                } catch (Exception ex) {
                    Logger.getLogger(RunOracleCandidateExpasions.class.getName()).log(Level.SEVERE, "error in running for "
                            + q.toString(), ex);
                    throw new IOException();
                }

                // if we have some results -- print in to output stream
                if (!results.isEmpty()) {
                    for (ScoredDocument sd : results) {
                        writer.write(sd.toTRECformat(queryNumber));
                        writer.newLine();
                    }
                } else {
                    writer.write(String.format("%s Q0 clueweb09-xxxxxx-xx-xxxxx 1 -1 galago", queryNumber));
                    writer.newLine();
                }
            }

            writer.close();
            Utility.infoWritten(rankFile);

            processor.process(folder);
        }

        public static String getPrmQuery(String text, double fbOrigWt, long fbDoc, long fbTerm) {
            //* #rm:fbOrigWt=0.5:fbDocs=10:fbTerms=10( query )
            return String.format("#rm:fbOrigWt=%f:fbDocs=%d:fbTerms=%d( #sdm( %s ))",
                    fbOrigWt, fbDoc, fbTerm, text);
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    public static class RunEvalForTuning extends StandardStep<TfFolder, TfFolder> {

        Parameters p;
        PrmDirectory prmDir;
        PrmParameterGenerator paramGen;
        TrecEvaluator evaluator;
        String qrelFileName;

        public RunEvalForTuning(TupleFlowParameters parameters) throws Exception {
            p = parameters.getJSON();
            paramGen = new PrmParameterGenerator(p);
            prmDir = new PrmDirectory(p);
            evaluator = new TrecEvaluator(p.getString("trecEval"));
            qrelFileName = p.getString("qrelTopic");
        }

        @Override
        public void process(TfFolder folder) throws IOException {
            Utility.infoProcessing(folder);
            String[] params = Utility.splitParameters(folder.id);
            String folderId = params[0];
            double fbOrigWt = Double.parseDouble(params[1]);
            long fbDoc = Long.parseLong(params[2]);
            long fbTerm = Long.parseLong(params[3]);

            File rankFile = prmDir.getRankFile(folderId, fbOrigWt, fbDoc, fbTerm);
            File evalFile = prmDir.getEvalFile(folderId, fbOrigWt, fbDoc, fbTerm);
            File tevalFile = prmDir.getTevalFile(folderId, fbOrigWt, fbDoc, fbTerm);

            if (evalFile.exists()) {
                Utility.infoFileExists(evalFile);
                processor.process(folder);
                return;
            }

            try {
                Utility.infoOpen(evalFile);
                Utility.infoOpen(tevalFile);
                evaluator.evalAndOutput(qrelFileName, rankFile.getAbsolutePath(), tevalFile, evalFile);
            } catch (Exception ex) {
                Logger.getLogger(TunePRM.class.getName()).log(Level.SEVERE, null, ex);
                throw new IOException();
            }
            Utility.infoWritten(evalFile);
            Utility.infoWritten(tevalFile);

            processor.process(folder);
        }
    }

}
