package edu.umass.ciir.fws.feature;

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
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * Tupleflow application that crawls top ranked documents for each query. Read
 * from index and write HTML files to document directory.
 *
 * @author wkong
 */
public class ExtractTermFeature extends AppFunction {

    private static final String name = "extract-term-feature";

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
//        assert (p.isString("queryFile")) : "missing input file, --input";
//        assert (p.isString("index")) : "missing --index";
//        assert (p.isString("rankedListFile")) : "missing --rankedListFile";
//        assert (p.isString("topNum")) : "missing --topNum";
//        assert (p.isString("docDir")) : "missing --docDir";

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

        stage.addOutput("praseQueries", new Query.IdOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(Utility.getSorter(new FileName.FilenameOrder()));
        stage.add(new Step(QueryFileParser.class));
        stage.add(Utility.getSorter(new Query.IdOrder()));
        stage.add(new OutputStep("praseQueries"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("praseQueries", new Query.IdOrder());

        stage.add(new InputStep("praseQueries"));
        stage.add(new Step(TermFeaturesExtractor.class, parameters));
        return stage;
    }
}
