    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfFolder;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
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
 * tune gmi for different evaluation metrics
 * @author wkong
 */
public class GmiTuneFacet extends AppFunction {

    @Override
    public String getName() {
        return "tune-facet-gmi";
    }

    @Override
    public String getHelpString() {
        return "fws " + getName() + " [parameters...]\n"
                + AppFunction.getTupleFlowParameterString();
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitEvalStage(parameters));
        job.add(getEvalStage(parameters));
        job.add(getSelectStage(parameters));
        job.add(getProcessStage(parameters));

        job.connect("splitEval", "eval", ConnectionAssignmentType.Each);
        job.connect("eval", "select", ConnectionAssignmentType.Combined);
        job.connect("select", "process", ConnectionAssignmentType.Each);

        return job;
    }

    private Stage getSplitEvalStage(Parameters parameters) {
        Stage stage = new Stage("splitEval");

        stage.addOutput("folderParams", new TfFolder.IdOrder());

        stage.add(new Step(SplitFoldersForTuneEval.class, parameters));
        stage.add(Utility.getSorter(new TfFolder.IdOrder()));
        stage.add(new OutputStep("folderParams"));

        return stage;
    }

    private Stage getEvalStage(Parameters parameters) {
        Stage stage = new Stage("eval");

        stage.addInput("folderParams", new TfFolder.IdOrder());
        stage.addOutput("folderParams2", new TfFolder.IdOrder());

        stage.add(new InputStep("folderParams"));
        stage.add(new Step(EvalTuneGmi.class, parameters));
        stage.add(Utility.getSorter(new TfFolder.IdOrder()));
        stage.add(new OutputStep("folderParams2"));
        return stage;
    }

    private Stage getSelectStage(Parameters parameters) {
        Stage stage = new Stage("select");

        stage.addInput("folderParams2", new TfFolder.IdOrder());
        stage.addOutput("selectedParams", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("folderParams2"));
        stage.add(new Step(SelectBestParam.class, parameters));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("selectedParams"));
        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("selectedParams", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("selectedParams"));
        stage.add(new Step(GmiClusterer.class, parameters));
        stage.add(new Step(GmiClusterToFacetConverter.class, parameters));
        stage.add(new Step(GmLearn.DoNonethingForQueryParams.class));

        return stage;
    }

    @Verified
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    public static class SplitFoldersForTuneEval implements ExNihiloSource<TfFolder> {

        public Processor<TfFolder> processor;
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
        public void run() throws IOException {
            for (int i = 1; i <= numFolders; i++) {
                String folderId = String.valueOf(i);
                for (double termTh : termProbThs) {
                    for (double pairTh : pairProbThs) {
                        for (String ranker : new String[]{"sum", "avg"}) {
                            String newParams = Utility.parametersToString(folderId, "tune", termTh, pairTh, ranker);
                            processor.process(new TfFolder(newParams));
                        }
                    }
                }
            }
            processor.close();
        }

        public void close() throws IOException {
            processor.close();
        }

        @Override
        public void setProcessor(org.lemurproject.galago.tupleflow.Step nextStage) throws IncompatibleProcessorException {
            Linkage.link(this, nextStage);
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SelectBestParam extends StandardStep<TfFolder, TfQueryParameters> {

        long numFolders;
        List<Double> termProbThs;
        List<Double> pairProbThs;
        String trainDir;
        String[] rankers = new String[]{"sum", "avg"};
        List<Long> metricIndices;
        int facetTuneRank;
        BufferedWriter writer;

        public SelectBestParam(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            numFolders = parameters.getJSON().getLong("cvFolderNum");
            termProbThs = p.getAsList("gmiTermProbThesholds", Double.class);
            pairProbThs = p.getAsList("gmiPairProbThesholds", Double.class);
            facetTuneRank = new Long(p.getLong("facetTuneRank")).intValue();
            String gmDir = p.getString("gmDir");
            trainDir = Utility.getFileName(gmDir, "train");
            metricIndices = p.getAsList("facetTuneMetricIndices", Long.class);
            writer = Utility.getWriter(p.getString("gmiTunedParamFile"));
        }

        @Override
        public void process(TfFolder object) throws IOException {
        }

        @Override
        public void close() throws IOException {

            for (Long metricIdx : metricIndices) {
                for (int i = 1; i <= numFolders; i++) {
                    for (String ranker : rankers) {
                        findBestParamAndEmitRun(String.valueOf(i), ranker, metricIdx.intValue());
                    }
                }
            }

            processor.close();
            writer.close();
        }

        private void findBestParamAndEmitRun(String folderId, String ranker, int metricIndex) throws IOException {
            String folderDir = Utility.getFileName(trainDir, folderId);
            String evalDir = Utility.getFileName(folderDir, "eval");

            double maxScore = Double.NEGATIVE_INFINITY;
            double maxScoreTermTh = -1;
            double maxScorePairTh = -1;
            for (double termTh : termProbThs) {
                for (double pairTh : pairProbThs) {
                    String param = Utility.parametersToFileNameString(termTh, pairTh, ranker);
                    File evalFile = new File(Utility.getFacetEvalFileName(evalDir, "gmi", param, facetTuneRank));
                    double score = QueryMetrics.getAvgScore(evalFile, metricIndex);
                    if (score > maxScore) {
                        maxScore = score;
                        maxScoreTermTh = termTh;
                        maxScorePairTh = pairTh;
                    }
                }
            }

            writer.write(String.format("gmi\t%s\t%s\t%d\t%f\t%f\n", folderId, ranker,
                    metricIndex, maxScoreTermTh, maxScorePairTh));

            String testQueryFileName = Utility.getFileName(folderDir, "test.query");
            TfQuery[] queries = QueryFileParser.loadQueryList(testQueryFileName);
            for (TfQuery q : queries) {
                String params = Utility.parametersToString(folderDir, "predict", maxScoreTermTh, maxScorePairTh, ranker, metricIndex);
                processor.process(new TfQueryParameters(q.id, q.text, params));
            }

        }

    }
}
