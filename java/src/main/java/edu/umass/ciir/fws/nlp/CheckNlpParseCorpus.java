package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.clist.CandidateListHtmlExtractor;
import edu.umass.ciir.fws.clist.CandidateListTextExtractor;
import edu.umass.ciir.fws.clist.ExtractCandidateListCorpus;
import edu.umass.ciir.fws.types.TfCandidateList;
import edu.umass.ciir.fws.types.TfDocumentName;
import edu.umass.ciir.fws.types.TfQueryDocumentName;
import java.io.BufferedWriter;
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
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.StandardStep;
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
 * Check parsed file for the corpus.
 *
 * @author wkong
 */
public class CheckNlpParseCorpus extends AppFunction {

    private static final String name = "check-parse-corpus";

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
        job.add(getWriteStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);
        job.connect("process", "write", ConnectionAssignmentType.Combined);

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
        stage.addOutput("checkedDocNames", new TfDocumentName.NameOrder());

        stage.add(new InputStep("docNames"));
        stage.add(new Step(CheckParsedDocument.class, parameters));
        stage.add(new OutputStep("checkedDocNames"));
        return stage;
    }

    private Stage getWriteStage(Parameters parameters) {
        Stage stage = new Stage("write");
        stage.addInput("checkedDocNames", new TfDocumentName.NameOrder());

        stage.add(new InputStep("checkedDocNames"));
        stage.add(new Step(DocumentNameWriter.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfDocumentName")
    public static class DocumentNameWriter implements Processor<TfDocumentName> {

        String docNameFixFile = "../data/doc-name/doc-name-need-process-again";
        BufferedWriter writer;

        public DocumentNameWriter(TupleFlowParameters parameters) throws IOException {
            writer = edu.umass.ciir.fws.utility.Utility.getWriter(docNameFixFile);
        }

        @Override
        public void process(TfDocumentName documentName) throws IOException {
            writer.write(documentName.name);
            writer.newLine();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

    }

    /**
     * Candidate lists extractor that will be called by Tupleflow jobs to
     * extract candidate lists.
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfDocumentName", order = {"+name"})
    @OutputClass(className = "edu.umass.ciir.fws.types.TfDocumentName", order = {"+name"})
    public static abstract class CheckParsedDocument extends StandardStep<TfDocumentName, TfDocumentName> {

        String parseCorpusDir;
        Retrieval retrieval;

        public CheckParsedDocument(TupleFlowParameters parameters) throws Exception {
            Parameters p = parameters.getJSON();
            parseCorpusDir = p.getString("parseCorpusDir");
        }

        @Override
        public void process(TfDocumentName docName) throws IOException {
            System.err.println("processing " + docName.name);

            // extract by text patterns
            String parseFileName = edu.umass.ciir.fws.utility.Utility.getParsedCorpusDocFileName(parseCorpusDir, docName.name);
            try {
                String parseFileContent = edu.umass.ciir.fws.utility.Utility.readFileToString(new File(parseFileName));
            } catch (IOException ex) {  
                System.err.println(ex.toString());
                processor.process(docName);
            }

            System.err.println("Done text");
        }
    }
}