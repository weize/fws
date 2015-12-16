package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.types.TfDocumentName;
import edu.umass.ciir.fws.types.TfQueryDocumentName;
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

/**
 * Tupleflow application for parsing documents in the corpus.
 *
 * @author wkong
 */
public class NlpParseCorpus extends AppFunction {

    private static final String name = "parse-corpus";

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
//        assert (p.isString("docNameFile")) : "missing input file, --docNameFile";
//        assert (p.isString("parseCorpusDir")) : "missing --parseCorpusDir";
//        assert (p.isString("index")) : "missing --index";
        
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

        stage.addOutput("docNames", new TfDocumentName.NameOrder());

        List<String> inputFiles = parameter.getAsList("docNameFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(DocumentNameFileParser.class));
        stage.add(Utility.getSorter(new TfDocumentName.NameOrder()));
        stage.add(new OutputStep("docNames"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("docNames", new TfDocumentName.NameOrder());

        stage.add(new InputStep("docNames"));
        stage.add(new Step(DocumentCorpusNLPParser.class, parameters));
        return stage;
    }
}
