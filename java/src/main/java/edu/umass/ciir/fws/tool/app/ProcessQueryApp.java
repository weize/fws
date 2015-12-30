/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.tool.app;

import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfFolder;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * A template AppFunction class for App that takes in queryFile, splits as
 * query, and then process for each query.
 *
 * @author wkong
 */
public abstract class ProcessQueryApp extends AppFunction {

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

        stage.addOutput("queries", new TfQuery.IdOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(Utility.getSorter(new FileName.FilenameOrder()));
        stage.add(new Step(QueryFileParser.class));
        stage.add(Utility.getSorter(new TfQuery.IdOrder()));
        stage.add(new OutputStep("queries"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("queries", new TfQuery.IdOrder());

        stage.add(new InputStep("queries"));
        stage.add(new Step(getProcessClass(), parameters));
        return stage;
    }

    /**
     * Subclass use this function to specify the process class that takes in a
     * queryParameter, and process.
     *
     * @return
     */
    protected abstract Class getProcessClass();

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    public static class DoNonething implements Processor<TfQuery> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void process(TfQuery object) throws IOException {
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolder")
    public static class DoNonethingForFolder implements Processor<TfFolder> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void process(TfFolder object) throws IOException {
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class DoNonethingForQueryParams implements Processor<TfQueryParameters> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void process(TfQueryParameters object) throws IOException {
        }
    }
}
