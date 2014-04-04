/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.tool;

import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.Query;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.tupleflow.execution.*;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 *
 * @author wkong
 */
public class TupleFlowTestFn extends AppFunction {

    private static final String name = "test-tupleflow";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHelpString() {
        return "fws " + name + " [parameters...]\n"
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
        job.add(getParseStage(parameters));
        job.add(getExtractStage(parameters));

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("splitFiles", new FileName.FilenameOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(Utility.getSorter(new FileName.FilenameOrder()));
        stage.add(new OutputStep("splitFiles"));

        return stage;
    }

    private Stage getParseStage(Parameters parameters) {
        Stage stage = new Stage("parse");

        stage.addInput("splitFiles", new FileName.FilenameOrder());
        stage.addOutput("praseQueries", new Query.IdOrder());

        stage.add(new InputStep("splitFiles"));
        stage.add(new Step(QueryFileParser.class));
        stage.add(Utility.getSorter(new Query.IdOrder()));
        stage.add(new OutputStep("praseQueries"));

        return stage;
    }

    private Stage getExtractStage(Parameters parameters) {
        Stage stage = new Stage("extract");

        stage.addInput("praseQueries", new Query.IdOrder());
        stage.addOutput("praseQueries", new Query.IdOrder());

        return stage;
    }
}
