package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.nlp.*;
import edu.umass.ciir.fws.types.DocumentName;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Tupleflow application for parsing documents in the corpus.
 *
 * @author wkong
 */
public class ExtractCandidateListCorpus extends AppFunction {

    private static final String name = "extract-clist-corpus";

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
        job.add(getProcessStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("docNames", new DocumentName.NameOrder());

        List<String> inputFiles = parameter.getAsList("docNameFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(DocumentNameFileParser.class));
        stage.add(Utility.getSorter(new DocumentName.NameOrder()));
        stage.add(new OutputStep("docNames"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("docNames", new DocumentName.NameOrder());

        stage.add(new InputStep("docNames"));
        stage.add(new Step(DocumentCorpusNLPParser.class, parameters));
        return stage;
    }

    /**
     * Candidate lists extractor that will be called by Tupleflow jobs to
     * extract candidate lists.
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.DocumentName")
    public static class CandidateListCorpusExtractor implements Processor<DocumentName> {

        CandidateListHtmlExtractor cListHtmlExtractor;
        CandidateListTextExtractor cListTextExtractor;
        String parseCorpusDir;
        Retrieval retrieval;
        String clistCorpusDir;

        public CandidateListCorpusExtractor(TupleFlowParameters parameters) throws Exception {
            Parameters p = parameters.getJSON();
            parseCorpusDir = p.getString("parseCorpusDir");
            clistCorpusDir = p.getString("clistCorpusDir");
            retrieval = RetrievalFactory.instance(p);
            cListHtmlExtractor = new CandidateListHtmlExtractor();
            cListTextExtractor = new CandidateListTextExtractor();
        }

        @Override
        public void process(DocumentName docName) throws IOException {
            System.err.println("processing " + docName.name);

            ArrayList<CandidateList> clists = new ArrayList<>();

            // extract by html patterns
            Document doc = retrieval.getDocument(docName.name, new Document.DocumentComponents(true, false, false));
            clists.addAll(cListHtmlExtractor.extract(doc.text, 0, "0")); // dummy docRank and qid
            System.err.println("Done html");

            // extract by text patterns
            String parseFileName = Utility.getParsedCorpusDocFileName(parseCorpusDir, docName.name);
            String parseFileContent = Utility.readFileToString(new File(parseFileName));
            clists.addAll(cListTextExtractor.extract(parseFileContent, 0, "0"));
            System.err.println("Done text");

            File outfile = new File(Utility.getCandidateListRawFileName(clistCorpusDir, query.id));
            edu.umass.ciir.fws.utility.Utility.createDirectoryForFile(outfile);
            CandidateList.output(clists, outfile);
            edu.umass.ciir.fws.utility.Utility.infoWritten(outfile);
        }

        @Override
        public void close() throws IOException {
        }
    }

}
