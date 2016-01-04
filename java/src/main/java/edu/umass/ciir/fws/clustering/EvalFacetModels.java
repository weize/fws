/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.eval.CombinedFacetEvaluator;
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
 *
 * @author wkong
 */
public class EvalFacetModels extends AppFunction {

    @Override
    public String getName() {
        return "eval-facets";
    }

    @Override
    public String getHelpString() {
        return "fws eval-facets config.json\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getEvalStage(parameters));

        job.connect("split", "eval", ConnectionAssignmentType.Each);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("modelParams", new TfQueryParameters.IdParametersOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(SplitEvalRuns.class, parameter));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("modelParams"));

        return stage;
    }

    private Stage getEvalStage(Parameters parameters) {
        Stage stage = new Stage("eval");

        stage.addInput("modelParams", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("modelParams"));
        stage.add(new Step(EvalFacetModel.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class EvalFacetModel implements Processor<TfQueryParameters> {

        CombinedFacetEvaluator evaluator;
        String allFacetDir;
        File queryFile;

        public EvalFacetModel(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            allFacetDir = p.getString("facetTuneDir");
            evaluator = new CombinedFacetEvaluator(p);
            queryFile = new File(p.getString("queryFile"));
        }

        @Override
        public void process(TfQueryParameters run) throws IOException {
            Utility.infoProcessing(run);
            String model = run.id;
            int numTopFacets = Integer.parseInt(run.text);
            String facetParam = run.parameters;
            String evalDir = Utility.getFileName(allFacetDir, model, "eval");
            String facetDir = Utility.getFileName(allFacetDir, model, "facet");

            File evalFile = new File(Utility.getFacetEvalFileName(evalDir, model, facetParam, numTopFacets));
            evaluator.eval(queryFile, facetDir, model, facetParam, evalFile, numTopFacets);
            Utility.infoWritten(evalFile);
        }

        @Override
        public void close() throws IOException {
        }
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SplitEvalRuns extends StandardStep<FileName, TfQueryParameters> {

        Parameters p;

        public SplitEvalRuns(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();

        }

        @Override
        public void process(FileName file) throws IOException {
        }

        @Override
        public void close() throws IOException {
            List<String> models = p.getAsList("facetModelsToEval");
            List<Long> facetTuneMetricIndices = p.getAsList("facetTuneMetricIndices");
            String allFacetDir = p.getString("facetTuneDir");
            int topFacetNum = (int) p.getLong("topFacetNum");
            for (String model : models) {
                String evalDir = Utility.getFileName(allFacetDir, model, "eval");
                Utility.createDirectory(evalDir);
                List<ModelParameters> paramsList = ParameterSettings.instance(p, model).
                        getTuningSettings(facetTuneMetricIndices);
                // 
                int start = topFacetNum > 100 ? topFacetNum : 1;
                for (int topFacets = start; topFacets <= topFacetNum; topFacets++) {
                    for (ModelParameters params : paramsList) {
                        processor.process(new TfQueryParameters(model,
                                String.valueOf(topFacets), params.toFilenameString()));
                    }
                }
            }

            processor.close();
        }

    }

}
