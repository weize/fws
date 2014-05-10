package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.clist.CandidateListParser;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfCandidateList;
import edu.umass.ciir.fws.types.TfTerm;
import edu.umass.ciir.fws.types.TfTermCount;
import java.io.BufferedWriter;
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
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Tupleflow application that fetch document frequency for all terms in the
 * candidate list set across all queries.
 *
 * @author wkong
 */
public class ExtractClueWebDocFreq extends AppFunction {

    private static final String name = "extract-clueweb-doc-freq";

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
        assert (p.isString("queryFile")) : "missing input file, --input";
        assert (p.isString("index")) : "missing --index";
        assert (p.isString("clistDir")) : "missing --clistDir";
        assert (p.isString("clueDfFile")) : "missing --clueDfFile";

        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getProcessStage(parameters));
        job.add(getWriteStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);
        job.connect("process", "write", ConnectionAssignmentType.Combined);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("terms", new TfTerm.TermOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(QueryFileParser.class));
        stage.add(new Step(CandidateListParser.class, parameter));
        stage.add(new Step(CandidateListToTerms.class));
        stage.add(Utility.getSorter(new TfTerm.TermOrder()));
        stage.add(new Step(TermUniqueReducer.class));
        stage.add(new OutputStep("terms"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("terms", new TfTerm.TermOrder());
        stage.addOutput("termCounts", new TfTermCount.TermOrder());

        stage.add(new InputStep("terms"));
        stage.add(new Step(GalagoDocFreqExtractor.class, parameters));
        stage.add(new OutputStep("termCounts"));

        return stage;
    }

    private Stage getWriteStage(Parameters parameters) {
        Stage stage = new Stage("write");

        stage.addInput("termCounts", new TfTermCount.TermOrder());

        stage.add(new InputStep("termCounts"));
        stage.add(new Step(TermCountWriter.class, parameters));

        return stage;
    }

    /**
     * Tupleflow writer that write term count into one file.
     *
     * @author wkong
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TermCount")
    public static class TermCountWriter implements Processor<TfTermCount> {

        String clueDfFile;
        TfCandidateList last = null;
        BufferedWriter writer;
        String suffix;

        public TermCountWriter(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            clueDfFile = p.getString("clueDfFile");
            writer = edu.umass.ciir.fws.utility.Utility.getGzipWriter(clueDfFile);
        }

        @Override
        public void process(TfTermCount termCount) throws IOException {
            writer.write(String.format("%s\t%d\n", termCount.term, termCount.count));
        }

        @Override
        public void close() throws IOException {
            writer.close();
            System.err.println("Written in " + clueDfFile);
        }

    }

}
