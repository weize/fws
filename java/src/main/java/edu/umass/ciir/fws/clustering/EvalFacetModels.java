/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.clustering.gm.GmLearn;
import edu.umass.ciir.fws.eval.QueryFacetEvaluator;
import edu.umass.ciir.fws.types.TfFolder;
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
        return "fws eval-facet config.json\n";
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
        stage.add(new Step(TuneFacetModel.CopyRun.class, parameters));
        stage.add(new Step(GmLearn.DoNonethingForQueryParams.class));
        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public class EvalFacetModel extends StandardStep<TfQueryParameters, TfQueryParameters> {

        QueryFacetEvaluator evaluator;
        String allFacetDir;

        public EvalFacetModel(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            allFacetDir = p.getString("facetDir");
            File facetJsonFile = new File(p.getString("facetAnnotationJson"));
            evaluator = new QueryFacetEvaluator(10, facetJsonFile);
        }

        @Override
        public void process(TfQueryParameters folder) throws IOException {
            Utility.infoProcessing(folder);
            String[] params = Utility.splitParameters(folder.id);
            String folderId = params[0];
            String predictOrTune = params[1];

            String folderDir = Utility.getFileName(tuneDir, folderId);
            String evalDir = Utility.getFileName(folderDir, "eval");
            File trainQueryFile = new File(Utility.getFileName(folderDir, "train.query"));

            String param = "";

            if (model.equals("plsa")) {
                long topicNum = Long.parseLong(params[2]);
                long termNum = Long.parseLong(params[3]);
                param = Utility.parametersToFileNameString(topicNum, termNum);
            } else if (model.equals("lda")) {
                long topicNum = Long.parseLong(params[2]);
                long termNum = Long.parseLong(params[3]);
                param = Utility.parametersToFileNameString(topicNum, termNum);

            } else if (model.equals("qd")) {
                double qdDistanceMax = Double.parseDouble(params[2]);
                double qdWebsiteCountMin = Double.parseDouble(params[3]);
                double qdItemRatio = Double.parseDouble(params[4]);
                param = Utility.parametersToFileNameString(qdDistanceMax, qdWebsiteCountMin, qdItemRatio);
            }

            File evalFile = new File(Utility.getFacetEvalFileName(evalDir, model, param));

            if (evalFile.exists()) {
                Utility.infoFileExists(evalFile);
                processor.process(folder);
                return;
            }

            evaluator.eval(trainQueryFile, runFacetDir, model, param, evalFile);
            Utility.infoWritten(evalFile);
            processor.process(folder);
        }
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolder")
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

            for (String model : models) {
                if (model.equals("plsa") || model.equals("lda") || model.equals("qd")) {
                    for (long idx : facetTuneMetricIndices) {
                        String params = Utility.parametersToString(model, idx);
                        processor.process(new TfQueryParameters("0", "", params));
                    }
                } else if (model.equals("gmj")) {
                    processor.process(new TfQueryParameters("0", "", "sum"));
                    processor.process(new TfQueryParameters("0", "", "avg"));
                } else if (model.equals("gmi")) {
                    for (long idx : facetTuneMetricIndices) {
                        String params;
                        params = Utility.parametersToString(model, "sum", idx);
                        processor.process(new TfQueryParameters("0", "", params));

                        params = Utility.parametersToString(model, "avg", idx);
                        processor.process(new TfQueryParameters("0", "", params));
                    }

                } else {
                    throw new IOException("cannot recognize " + model);
                }
            }

            processor.close();
        }

    }

}
