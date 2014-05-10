package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.nlp.*;
import edu.umass.ciir.fws.types.CandidateList;
import edu.umass.ciir.fws.types.DocumentName;
import edu.umass.ciir.fws.utility.Utility;
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
        job.add(getWriteStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);
        job.connect("process", "write", ConnectionAssignmentType.Combined);

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
        stage.addOutput("clists", new CandidateList.QidDocRankDocNameListTypeItemListOrder());

        stage.add(new InputStep("docNames"));
        stage.add(new Step(CandidateListCorpusExtractor.class, parameters));
        stage.add(Utility.getSorter(new CandidateList.QidDocRankDocNameListTypeItemListOrder()));
        stage.add(new OutputStep("clists"));
        return stage;
    }

    private Stage getWriteStage(Parameters parameters) {
        Stage stage = new Stage("write");
        stage.addInput("clists", new CandidateList.QidDocRankDocNameListTypeItemListOrder());

        stage.add(new InputStep("clists"));
        stage.add(new Step(CandidateListCorpusWriter.class, parameters));
        return stage;
    }

    /**
     * Candidate lists extractor that will be called by Tupleflow jobs to
     * extract candidate lists.
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.CandidateList", order = {"+qid", "+docRank", "+docName", "+listType", "+itemList"})
    public static class CandidateListCorpusWriter implements Processor<CandidateList> {

        String fileName; // current filename
        String clistCorpusDir;
        BufferedWriter writer;

        public CandidateListCorpusWriter(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            clistCorpusDir = p.getString("clistCorpusDir");
            fileName = null;
        }

        @Override
        public void process(CandidateList clist) throws IOException {
            String newFileName = Utility.getCorpusCandidateListFileName(clistCorpusDir, clist.docName);
            if (fileName == null) {
                onNewFile(newFileName);
            } else {
                if (!newFileName.equals(fileName)) {
                    writer.close();
                    onNewFile(newFileName);
                }
            }

            writeClist(clist);
        }

        @Override
        public void close() throws IOException {
            if (writer != null) {
                writer.close();
            }
        }

        private void writeClist(CandidateList clist) throws IOException {
            writer.write(clist.toString());
            writer.newLine();
        }

        private void onNewFile(String newFileName) throws IOException {
            fileName = newFileName;
            Utility.createDirectoryForFile(fileName);
            writer = Utility.getGzipWriter(fileName);
        }

    }

    /**
     * Candidate lists extractor that will be called by Tupleflow jobs to
     * extract candidate lists.
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.DocumentName")
    @OutputClass(className = "edu.umass.ciir.fws.types.CandidateList")
    public static class CandidateListCorpusExtractor extends StandardStep<DocumentName, CandidateList> {

        CandidateListHtmlExtractor cListHtmlExtractor;
        CandidateListTextExtractor cListTextExtractor;
        String parseCorpusDir;
        Retrieval retrieval;
        final static long docRank = 0; // dummy docRank
        final static String qid = "0"; // dummy qid

        public CandidateListCorpusExtractor(TupleFlowParameters parameters) throws Exception {
            Parameters p = parameters.getJSON();
            parseCorpusDir = p.getString("parseCorpusDir");
            retrieval = RetrievalFactory.instance(p);
            cListHtmlExtractor = new CandidateListHtmlExtractor();
            cListTextExtractor = new CandidateListTextExtractor();
        }

        @Override
        public void process(DocumentName docName) throws IOException {
            System.err.println("processing " + docName.name);

            // extract by html patterns
            Document doc = retrieval.getDocument(docName.name, new Document.DocumentComponents(true, false, false));
            for (edu.umass.ciir.fws.clist.CandidateList clist : cListHtmlExtractor.extract(doc.text, docRank, docName.name, qid)) {
                processor.process(new CandidateList(clist.qid, clist.docRank, clist.docName, clist.listType, clist.itemList));
            }
            System.err.println("Done html");

            // extract by text patterns
            String parseFileName = Utility.getParsedCorpusDocFileName(parseCorpusDir, docName.name);
            String parseFileContent = Utility.readFileToString(new File(parseFileName));
            for (edu.umass.ciir.fws.clist.CandidateList clist : cListTextExtractor.extract(parseFileContent, docRank, docName.name, qid)) {
                processor.process(new CandidateList(clist.qid, clist.docRank, clist.docName, clist.listType, clist.itemList));
            }
            System.err.println("Done text");
        }
    }

}
