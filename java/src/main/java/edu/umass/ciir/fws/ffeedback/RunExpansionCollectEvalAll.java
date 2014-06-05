/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
import edu.umass.ciir.fws.types.TfFacetFeedbackParams;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
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

/**
 *
 * @author wkong
 */
public class RunExpansionCollectEvalAll extends AppFunction {

    @Override
    public String getName() {
        return "run-expansion-collect-eval-all";
    }

    @Override
    public String getHelpString() {
        return "fws run-expansion-collect-all --expansionModel=";
    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getProcessStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);

        return job;
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("feedbacks", new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(CreateExpansionFileAll.SplitFeedbacks.class, parameter));
        stage.add(Utility.getSorter(new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder()));
        stage.add(new OutputStep("feedbacks"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameter) {
        Stage stage = new Stage("process");

        stage.addInput("feedbacks", new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder());

        stage.add(new InputStep("feedbacks"));
        stage.add(new Step(CollectTevals.class, parameter));
        stage.add(new Step(CollectTimeCostEvals.class, parameter));

        return stage;
    }

    /**
     * Read expansion from expansion file, and emit expansion with each
     * subtopics that has qrel in splitSqrel.
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFacetFeedbackParams")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFacetFeedbackParams")
    public static class CollectTevals extends StandardStep<TfFacetFeedbackParams, TfFacetFeedbackParams> {

        ExpansionDirectory expansionDir;
        String expansionModel;
        boolean first;
        String[] metrics;

        public CollectTevals(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            expansionDir = new ExpansionDirectory(p);
            expansionModel = p.getString("expansionModel");
            first = true;
        }

        @Override
        public void process(TfFacetFeedbackParams ffParam) throws IOException {
            Utility.infoProcessing(ffParam);
            File outfile = expansionDir.getExpansionEvalFile(ffParam, expansionModel);
            File expansionFie = expansionDir.getExpansionFile(ffParam, expansionModel);
            List<QuerySubtopicExpansion> qses = QuerySubtopicExpansion.load(expansionFie, expansionModel);
            
            Utility.infoOpen(outfile);
            BufferedWriter writer = Utility.getWriter(outfile);
            for (QuerySubtopicExpansion qse : qses) {
                File tevalFile = new File(Utility.getQExpSubtopicTevalFileName(expansionDir.evalDir, qse));
                TrecEvaluator evaluator = new TrecEvaluator(tevalFile);
                List<QueryMetrics> qms = evaluator.resultToQueryMetrics();

                // verify metrics are consistent across different evaluation files
                if (first) {
                    metrics = Arrays.copyOf(evaluator.metrics, evaluator.metrics.length);
                    first = false;
                    writer.write(evaluator.getHeader());
                    writer.newLine();
                } else {
                    assert metrics.length == evaluator.metrics.length : "number of metrics not match " + tevalFile.getAbsolutePath();
                    for (int i = 0; i < metrics.length; i++) {
                        assert metrics[i].equals(evaluator.metrics[i]) : "metrics not match " + tevalFile.getAbsolutePath();
                    }
                }

                for (QueryMetrics qm : qms) {
                    if (!qm.qid.equals("all")) {
                        qm.qid = QuerySubtopicExpansion.toId(qse);
                        writer.write(qm.toString());
                        writer.newLine();
                    }
                }
            }
            writer.close();
            Utility.infoWritten(outfile);
            processor.process(ffParam);
        }
    }

    /**
     * Read expansion from expansion file, and emit expansion with each
     * subtopics that has qrel in splitSqrel.
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFacetFeedbackParams")
    public static class CollectTimeCostEvals implements Processor<TfFacetFeedbackParams> {

        ExpansionTimeCostEvaluator timeCostEvaluator;
        
        public CollectTimeCostEvals(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            timeCostEvaluator = new ExpansionTimeCostEvaluator(p);
        }

        @Override
        public void process(TfFacetFeedbackParams ffParam) throws IOException {
            Utility.infoProcessing(ffParam);
            timeCostEvaluator.run(ffParam);
        }

        @Override
        public void close() throws IOException {
        }
    }

}
