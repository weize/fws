/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.query.QueryFileParser;
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
public class GmiClusterFacetsAfterTuning extends AppFunction {

    @Override
    public String getName() {
        return "gmi-cluster-after-tune";
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

        job.add(getSplitStage(parameters));
        job.add(getProcessStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("runs", new TfQueryParameters.IdParametersOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(SelectBestParam.class, parameter));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("runs"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("runs", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("runs"));
        stage.add(new Step(GmiClusterItems.class, parameters));
        stage.add(new Step(GmiClusterToFacetConverter.class, parameters));
        stage.add(new Step(GmLearn.DoNonethingForQueryParams.class));

        return stage;
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SelectBestParam extends StandardStep<FileName, TfQueryParameters> {

        long numFolders;
        List<Double> termProbThs;
        List<Double> pairProbThs;
        String trainDir;
        String[] rankers = new String[]{"sum", "avg"};
        List<Long> metricIndices;
        BufferedWriter writer;

        public SelectBestParam(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            numFolders = parameters.getJSON().getLong("cvFolderNum");
            termProbThs = p.getAsList("gmiTermProbThesholds", Double.class);
            pairProbThs = p.getAsList("gmiPairProbThesholds", Double.class);

            String gmDir = p.getString("gmDir");
            trainDir = Utility.getFileName(gmDir, "train");
            metricIndices = p.getAsList("facetTuneMetricIndices", Long.class);
            writer = Utility.getWriter(p.getString("gmiTunedParamFile"));
        }

        @Override
        public void process(FileName object) throws IOException {
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
                    File evalFile = new File(Utility.getFacetEvalFileName(evalDir, "gmi", param));
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
