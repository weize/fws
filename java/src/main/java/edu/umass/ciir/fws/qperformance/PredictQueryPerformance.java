/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.qperformance;

import edu.umass.ciir.fws.types.TfParameters;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
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
 * extract features for predicting query facet performance
 *
 * @author wkong
 */
public class PredictQueryPerformance extends AppFunction {

    @Override
    public String getName() {
        return "predict-qperformance";
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);
    }

    private Job createJob(Parameters p) {
        Job job = new Job();

        job.add(getSplitRunsStage(p));
        job.add(getProcessStage(p));

        job.connect("splitRuns", "process", ConnectionAssignmentType.Each);

        return job;

    }

    private Stage getSplitRunsStage(Parameters parameter) {
        Stage stage = new Stage("splitRuns");

        stage.addOutput("runs", new TfParameters.ParamsOrder());

        Parameters p = new Parameters();
        p.set("input", parameter.getString("qpRunFile"));

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(RunParser.class));
        stage.add(Utility.getSorter(new TfParameters.ParamsOrder()));
        stage.add(new OutputStep("runs"));

        return stage;
    }

    private Stage getProcessStage(Parameters p) {
        Stage stage = new Stage("process");

        stage.addInput("runs", new TfParameters.ParamsOrder());

        stage.add(new InputStep("runs"));
        stage.add(new Step(Classifier.class, p));

        return stage;
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfParameters")
    public static class RunParser extends StandardStep<FileName, TfParameters> {

        @Override
        public void process(FileName fileName) throws IOException {
            BufferedReader reader = Utility.getReader(fileName.filename);
            String line;
            while ((line = reader.readLine()) != null) {
                processor.process(new TfParameters(line));
            }
            reader.close();
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfParameters")
    public static class Classifier implements Processor<TfParameters> {

        QPClassifierCV classifier;
        String qpDir;
        String expDir;

        public Classifier(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            qpDir = p.getString("qpDir");
            expDir = p.getString("expDir");
            classifier = new QPClassifierCV(p);
        }

        @Override
        public void process(TfParameters params) throws IOException {
            String[] elems = params.params.split("\t");
            String qpRunId = elems[0];
            String facetRunId = elems[1]; // for gm directory
            String facetTuneId = elems[2];
            String facetModel = elems[3];
            String modelParams = elems[4];
            int metricIdx = Integer.parseInt(elems[5]);

            String facetTuneDir = Utility.getFileName(expDir, "facet-tune-" + facetTuneId);
            String gmPredictDir = Utility.getFileName(expDir, "facet-run-" + facetRunId, "gm", "predict");
            String subDir = qpRunId.split("-")[0];
            String qpRunDir = Utility.getFileName(qpDir, "run", subDir, qpRunId);
            classifier.run(facetTuneDir, facetModel, modelParams, gmPredictDir, metricIdx, qpRunDir);
        }

        @Override
        public void close() throws IOException {
        }
    }
}
