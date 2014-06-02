/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.clustering.EvalFacetModels;
import edu.umass.ciir.fws.clustering.FacetModelParamGenerator;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.eval.QueryFacetEvaluator;
import edu.umass.ciir.fws.types.TfParameters;
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
        return "fws extract-simulated-ffeedback config.json\n";
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

        stage.addOutput("params", new TfParameters.ParamsOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(SplitParams.class, parameter));
        stage.add(Utility.getSorter(new TfParameters.ParamsOrder()));
        stage.add(new OutputStep("params"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("params", new TfParameters.ParamsOrder());

        stage.add(new InputStep("params"));
        stage.add(new Step(ExtractFeedback.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfParameters")
    public static class SplitParams extends StandardStep<FileName, TfParameters> {

        Parameters p;
        FacetModelParamGenerator facetParam;

        public SplitParams(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
            facetParam = new FacetModelParamGenerator(p);

        }

        @Override
        public void process(FileName file) throws IOException {
        }

        @Override
        public void close() throws IOException {
            List<String> models = p.getAsList("facetModelsToSimulateFeedback");
            String allFeedbackDir = p.getString("feedbackDir");

            for (String model : models) {
                String fdbkDir = Utility.getFileName(allFeedbackDir, model);
                Utility.createDirectory(fdbkDir);
                for (String param : facetParam.getParams(model)) {
                    String newParam = Utility.parametersToString(model, param);
                    processor.process(new TfParameters(newParam));
                }
            }

            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfParameters")
    public static class ExtractFeedback implements Processor<TfParameters> {

        QueryFacetEvaluator evaluator;
        String allFacetDir;
        String allFeedbackDir;
        File annotatorFdbkFile;

        public ExtractFeedback(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            allFacetDir= p.getString("facetDir");
            allFeedbackDir= p.getString("feedbackDir");
            annotatorFdbkFile = new File(p.getString("annotatorFeedback"));
        }

        @Override
        public void process(TfParameters param) throws IOException {
            Utility.infoProcessing(param);
            String [] params = Utility.splitParameters(param.params);
            String model = params[0];
            String facetParam = Utility.parametersToString(Arrays.asList(params).subList(1, params.length));
            
            String facetDir = Utility.getFileName(allFacetDir, model, "facet");
            String feedbackDir = Utility.getFileName(allFeedbackDir, model);
            File outfile = new File(Utility.getAnnotatorFeedbackFileName(feedbackDir, model, facetParam));

            BufferedWriter writer = Utility.getWriter(outfile);
            List<FacetFeedback> anntatorFkList = FacetFeedback.load(annotatorFdbkFile);
            for (FacetFeedback anFk : anntatorFkList) {
                File facetFile = new File(Utility.getFacetFileName(facetDir, anFk.qid, model, facetParam));
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
