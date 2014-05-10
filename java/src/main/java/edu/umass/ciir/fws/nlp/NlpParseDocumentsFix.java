package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.types.TfQueryDocumentName;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
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
 * Tupleflow application for parsing documents of each query
 *
 * @author wkong
 */
public class NlpParseDocumentsFix extends AppFunction {

    private static final String name = "fix-parse-documents";

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
        assert (p.isString("rankedListFile")) : "missing --rankedListFile";
        assert (p.isString("topNum")) : "missing --topNum";
        assert (p.isString("parseDir")) : "missing --parseDir";
        assert (p.isString("docDir")) : "missing --docDir";

        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getFilterStage(parameters));
        job.add(getProcessStage(parameters));

        job.connect("split", "filter", ConnectionAssignmentType.Each);
        job.connect("filter", "process", ConnectionAssignmentType.Each);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("queryDocNames", new TfQueryDocumentName.QidDocNameOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(QueryFileDocumentsParser.class, parameter));
        stage.add(Utility.getSorter(new TfQueryDocumentName.QidDocNameOrder()));
        stage.add(new OutputStep("queryDocNames"));

        return stage;
    }

    private Stage getFilterStage(Parameters parameters) {
        Stage stage = new Stage("filter");

        stage.addInput("queryDocNames", new TfQueryDocumentName.QidDocNameOrder());
        stage.addOutput("queryDocNames2", new TfQueryDocumentName.QidDocNameOrder());

        stage.add(new InputStep("queryDocNames"));
        stage.add(new Step(QueryDocumentNameFilter.class, parameters));
        stage.add(Utility.getSorter(new TfQueryDocumentName.QidDocNameOrder()));
        stage.add(new OutputStep("queryDocNames2"));
        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("queryDocNames2", new TfQueryDocumentName.QidDocNameOrder());

        stage.add(new InputStep("queryDocNames2"));
        stage.add(new Step(DocumentNLPParser.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryDocumentName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryDocumentName")
    public static class QueryDocumentNameFilter extends StandardStep<TfQueryDocumentName, TfQueryDocumentName> {

        StanfordCoreNLPParser stanfordParser;
        String parseDir;
        String docDir;

        public QueryDocumentNameFilter(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            stanfordParser = new StanfordCoreNLPParser();
            parseDir = p.getString("parseDir");
            docDir = p.getString("docDir");
        }

        @Override
        public void process(TfQueryDocumentName queryDocName) throws IOException {
            String inputFileName = Utility.getDocHtmlFileName(
                    docDir, queryDocName.qid, queryDocName.docName);
            System.err.println("Filtering " + inputFileName);
            String contentNew = HtmlContentExtractor.extractFromFile(inputFileName);
            String contentOld = HtmlContentOldExtractor.extractFromFile(inputFileName);

            List<String> sentencesNew = stanfordParser.getAndOrSentences(contentNew);
            List<String> sentencesOld = stanfordParser.getAndOrSentences(contentOld);

            if (!isSame(sentencesNew, sentencesOld)) {
                processor.process(queryDocName);
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
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryDocumentName")
    public static class DoNonething implements Processor<TfQueryDocumentName> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void process(TfQueryDocumentName queryDocumentName) throws IOException {
        }
    }
}
