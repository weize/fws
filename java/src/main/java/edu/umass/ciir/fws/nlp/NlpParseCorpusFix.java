package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.types.TfDocumentName;
import edu.umass.ciir.fws.types.TfQueryDocumentName;
import edu.umass.ciir.fws.utility.TextProcessing;
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
public class NlpParseCorpusFix extends AppFunction {

    private static final String name = "fix-parse-corpus";

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
        assert (p.isString("docNameFile")) : "missing input file, --docNameFile";
        assert (p.isString("parseCorpusDir")) : "missing --parseCorpusDir";
        assert (p.isString("index")) : "missing --index";

        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getFilterStage(parameters));
        job.add(getWriteStage(parameters));

        job.connect("split", "filter", ConnectionAssignmentType.Each);
        job.connect("filter", "write", ConnectionAssignmentType.Combined);

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

    private Stage getFilterStage(Parameters parameters) {
        Stage stage = new Stage("filter");

        stage.addInput("docNames", new TfDocumentName.NameOrder());
        stage.addOutput("docNames2", new TfDocumentName.NameOrder());

        stage.add(new InputStep("docNames"));
        stage.add(new Step(DocumentNameFilter.class, parameters));
        stage.add(Utility.getSorter(new TfDocumentName.NameOrder()));
        stage.add(new OutputStep("docNames2"));
        return stage;
    }

    private Stage getWriteStage(Parameters parameters) {
        Stage stage = new Stage("write");

        stage.addInput("docNames2", new TfDocumentName.NameOrder());

        stage.add(new InputStep("docNames2"));
        stage.add(new Step(DocumentNameWriter.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.DocumentName")
    @OutputClass(className = "edu.umass.ciir.fws.types.DocumentName")
    public static class DocumentNameFilter extends StandardStep<TfDocumentName, TfDocumentName> {

        StanfordCoreNLPParser stanfordParser;
        String parseDir;
        String docDir;

        Parameters parameters;
        String parseCorpusDir;
        Retrieval retrieval;

        public DocumentNameFilter(TupleFlowParameters parameters) throws Exception {
            this.parameters = parameters.getJSON();
            stanfordParser = new StanfordCoreNLPParser();
            parseCorpusDir = this.parameters.getString("parseCorpusDir");
            retrieval = RetrievalFactory.instance(this.parameters);
        }

        @Override
        public void process(TfDocumentName docName) throws IOException {
            System.err.println("Filtering  " + docName.name);
            Document doc = retrieval.getDocument(docName.name, new Document.DocumentComponents(true, false, false));

            String contentNew = HtmlContentExtractor.extractFromContent(doc.text);
            String contentOld = HtmlContentOldExtractor.extractFromContent(doc.text);

            List<String> sentencesNew = stanfordParser.getAndOrSentences(contentNew);
            List<String> sentencesOld = stanfordParser.getAndOrSentences(contentOld);

            if (!isSame(sentencesNew, sentencesOld)) {
                processor.process(docName);
            }
        }

        private boolean isSame(List<String> s1, List<String> s2) {
            if (s1.size() != s2.size()) {
                System.err.println("Diff: size " + s1.size() + " " + s2.size());
                System.err.println(TextProcessing.join(s1, "\n") + "\n");
                System.err.println("----------------------------------------");
                System.err.println(TextProcessing.join(s2, "\n") + "\n");
                return false;
            } else {
                for (int i = 0; i < s1.size(); i++) {
                    if (!s1.get(i).equals(s2.get(i))) {
                        System.err.println("Diff: sentence " + i);
                        System.err.println(s1.get(i));
                        System.err.println(s2.get(i));
                        return false;
                    }
                }
                return true;
            }
        }
    }
    
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.DocumentName")
    public static class DocumentNameWriter implements Processor<TfDocumentName> {
        String docNameFixFile = "../data/doc-name/doc-name-fix";
        BufferedWriter writer;
        
        public DocumentNameWriter(TupleFlowParameters parameters) throws IOException {
            writer = Utility.getWriter(docNameFixFile);   
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
}
