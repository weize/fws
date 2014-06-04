/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.clustering.FacetModelParamGenerator;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.eval.QueryFacetEvaluator;
import edu.umass.ciir.fws.types.TfFacetFeedbackParams;
import edu.umass.ciir.fws.types.TfFeedbackParams;
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
public class ExtractSimulatedFfeedback extends AppFunction {

    @Override
    public String getName() {
        return "extract-simulated-ffeedback";
    }

    @Override
    public String getHelpString() {
        return "fws extract-simulated-ffeedback --feedbackSourceType=(oracle|annotator) config.json\n";
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

        stage.addOutput("params", new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(SplitFeedbacks.class, parameter));
        stage.add(new Step(SplitFacets.class, parameter));
        stage.add(Utility.getSorter(new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder()));
        stage.add(new OutputStep("params"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("params", new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder());

        stage.add(new InputStep("params"));
        stage.add(new Step(ExtractFeedback.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFeedbackParams")
    public static class SplitFeedbacks extends StandardStep<FileName, TfFeedbackParams> {

        Parameters p;
        FacetModelParamGenerator facetParam;

        public SplitFeedbacks(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();

        }

        @Override
        public void process(FileName file) throws IOException {
        }

        @Override
        public void close() throws IOException {
            FeedbackParameterGenerator paramGen = new FeedbackParameterGenerator(p);
            String feedbackType = p.getString("feedbackSource");

            List<String> params = paramGen.getParams(feedbackType);

            for (String param : params) {
                processor.process(new TfFeedbackParams(feedbackType, param));
            }

            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFeedbackParams")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFacetFeedbackParams")
    public static class SplitFacets extends StandardStep<TfFeedbackParams, TfFacetFeedbackParams> {

        Parameters p;
        FacetModelParamGenerator facetParam;

        public SplitFacets(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
            facetParam = new FacetModelParamGenerator(p);

        }

        @Override
        public void process(TfFeedbackParams fdbkParams) throws IOException {
            List<String> models = p.getAsList("facetModelsToSimulateFeedback");
            String allFeedbackDir = p.getString("feedbackDir");

            for (String model : models) {
                String fdbkDir = Utility.getFileName(allFeedbackDir, model);
                Utility.createDirectory(fdbkDir);
                for (String param : facetParam.getParams(model)) {
                    processor.process(new TfFacetFeedbackParams(model, param, fdbkParams.source, fdbkParams.params));
                }
            }
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFacetFeedbackParams")
    public static class ExtractFeedback implements Processor<TfFacetFeedbackParams> {

        QueryFacetEvaluator evaluator;
        String allFacetDir;
        String allFeedbackDir;

        public ExtractFeedback(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            allFacetDir = p.getString("facetDir");
            allFeedbackDir = p.getString("feedbackDir");
        }

        @Override
        public void process(TfFacetFeedbackParams param) throws IOException {
            Utility.infoProcessing(param);

            File srcFdbkFile = new File(Utility.getFeedbackFileName(allFeedbackDir, param.feedbackSource, "", param.feedbackSource, param.feedbackParams));
            String facetDir = Utility.getFileName(allFacetDir, param.facetSource, "facet");
            File outfile = new File(Utility.getFeedbackFileName(allFeedbackDir, param));

            Utility.infoOpen(outfile);
            List<FacetFeedback> anntatorFkList = FacetFeedback.load(srcFdbkFile);
            BufferedWriter writer = Utility.getWriter(outfile);
            for (FacetFeedback anFk : anntatorFkList) {
                File facetFile = new File(Utility.getFacetFileName(facetDir, anFk.qid, param.facetSource, param.facetParams));
                List<ScoredFacet> facets = ScoredFacet.loadFacets(facetFile);
                FacetFeedback simulatedFdbk = FacetFeedback.getSimulatedFfeedback(anFk, facets);
                writer.write(simulatedFdbk.toString());
                writer.newLine();
            }

            writer.close();
            Utility.infoWritten(outfile);
        }

        @Override
        public void close() throws IOException {
        }
    }

}
