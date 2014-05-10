package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.clist.CandidateListParser;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfCandidateList;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;

/**
 * Collect "candidate list" document frequency for all items in the candidate
 * list set. There are different versions of "candidate list" document
 * frequency, depends on what is treated as a "document" (a query, a document or
 * a candidate list).
 *
 * @author wkong
 */
public class ExtractCandidateListDocFreq extends AppFunction {

    private static final String name = "extract-clist-doc-freq";

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

        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getWriteStage(parameters));

        job.connect("split", "write", ConnectionAssignmentType.Combined);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("clists", new TfCandidateList.QidDocRankDocNameListTypeItemListOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(QueryFileParser.class));
        parameter.set("suffix", "clean.clist");
        stage.add(new Step(CandidateListParser.class, parameter));
        stage.add(Utility.getSorter(new TfCandidateList.QidDocRankDocNameListTypeItemListOrder()));
        stage.add(new OutputStep("clists"));

        return stage;
    }

    private Stage getWriteStage(Parameters parameters) {
        Stage stage = new Stage("write");

        stage.addInput("clists", new TfCandidateList.QidDocRankDocNameListTypeItemListOrder());

        stage.add(new InputStep("clists"));
        stage.add(new Step(CandidateListDocFreqWriter.class, parameters));

        return stage;
    }
}
