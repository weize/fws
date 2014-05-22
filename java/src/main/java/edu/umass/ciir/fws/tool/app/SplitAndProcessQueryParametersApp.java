/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.tool.app;

import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQueryParameters;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.Parameters;

import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * A template AppFunction class for App that takes in a file, splits as
 * queryParameters, and then process for each queryParameters.
 *
 * @author wkong
 */
public abstract class SplitAndProcessQueryParametersApp extends AppFunction {

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

    /**
     * Name of the input file.
     * @param parameter
     * @return 
     */
    public abstract String getInputFileName(Parameters parameter);

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("queryParameters", new TfQueryParameters.IdParametersOrder());

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        p.getList("input").add(new File(getInputFileName(parameter)).getAbsolutePath());

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(getQueryParametersGeneratorClass(), parameter));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("queryParameters"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("queryParameters", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("queryParameters"));
        stage.add(new Step(getProcessClass(), parameters));
        return stage;
    }

    /**
     * Subclass use this function to specify how query are converted to
     * queryParameters.
     *
     * @return
     */
    protected abstract Class getQueryParametersGeneratorClass();

    /**
     * Subclass use this function to specify the process class that takes in a
     * queryParameter, and process.
     *
     * @return
     */
    protected abstract Class getProcessClass();
}
