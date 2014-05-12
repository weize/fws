package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.types.TfCandidateList;
import edu.umass.ciir.fws.types.TfListItem;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * Collect "candidate list" document frequency for all items in the candidate
 * list corpus. There are different versions of "candidate list" document
 * frequency, depends on what is treated as a "document" (a query, a document or
 * a candidate list).
 *
 * @author wkong
 */
public class ExtractCandidateListDocFreqFromCorpus extends AppFunction {

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
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getProcessStage(parameters));
        job.add(getWriteStage(parameters));
        job.add(getWriteMetaStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);
        job.connect("process", "write", ConnectionAssignmentType.Combined);
        job.connect("process", "writeMeta", ConnectionAssignmentType.Combined);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("fileNames", new FileName.FilenameOrder());

        List<String> inputFiles = parameter.getAsList("clistCorpusDir");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(Utility.getSorter(new FileName.FilenameOrder()));
        stage.add(new OutputStep("fileNames"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("fileNames", new FileName.FilenameOrder());
        stage.addOutput("items", new TfListItem.TermDocNameListTypeOrder());

        stage.add(new InputStep("fileNames"));
        stage.add(new Step(CandidateListCorpusParser.class, parameters));
        stage.add(new Step(CandidateListToListItem.class));
        stage.add(Utility.getSorter(new TfListItem.TermDocNameListTypeOrder()));
        stage.add(new OutputStep("items"));
        return stage;
    }
    
    private Stage getWriteMetaStage(Parameters parameters) {
        Stage stage = new Stage("writeMeta");

        stage.addInput("items", new TfListItem.TermDocNameListTypeOrder());

        stage.add(new InputStep("items"));
        stage.add(Utility.getSorter(new TfListItem.DocNameListTypeOrder()));
        stage.add(new Step(CandidateListDocFreqMetaWriter.class, parameters));

        return stage;
    }
    
    private Stage getWriteStage(Parameters parameters) {
        Stage stage = new Stage("write");

        stage.addInput("items", new TfListItem.TermDocNameListTypeOrder());

        stage.add(new InputStep("items"));
        stage.add(new Step(CandidateListDocFreqWriter.class, parameters));

        return stage;
    }

    /**
     * Parse candidate lists from a file
     */
    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfCandidateList")
    public static class CandidateListCorpusParser extends StandardStep<FileName, TfCandidateList> {

        @Override
        public void process(FileName fileName) throws IOException {
            if (fileName.filename.endsWith(".clean.clist.gz")) {
                BufferedReader reader = Utility.getReader(fileName.filename);
                while (true) {
                    CandidateList clist = CandidateList.readOne(reader);
                    if (clist == null) { // end of file
                        break;
                    } else {
                        processor.process(clist.toTfCandidateList());
                    }
                }
                reader.close();
            }
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfCandidateList")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfListItem")
    public static class CandidateListToListItem extends StandardStep<TfCandidateList, TfListItem> {

        @Override
        public void process(TfCandidateList clist) throws IOException {
            for (String term : CandidateList.splitItemList(clist.itemList)) {
                TfListItem item = new TfListItem(term, clist.docName, clist.listType);
                processor.process(item);
            }

        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfListItem", order = {"+term", "+docName", "+listType"})
    public static class CandidateListDocFreqWriter implements Processor<TfListItem> {

        BufferedWriter writer;
        TfListItem last;
        HashMap<String, String> lastDocNames;
        HashMap<String, Long> tfs; // term freq
        HashMap<String, Long> dfs; // doc freq

        public CandidateListDocFreqWriter(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            String clistDfFile = p.getString("clistDfFile");
            writer = Utility.getWriter(clistDfFile);
            lastDocNames = new HashMap<>();
            tfs = new HashMap<>();
            dfs = new HashMap<>();
        }

        @Override
        public void process(TfListItem item) throws IOException {
            // a new term
            if (last != null && !item.term.equals(last.term)) {
                writeCounts();
                lastDocNames.clear();
                tfs.clear();
                dfs.clear();
            }

            for (String type : new String[]{"all", item.listType}) {
                addOne(tfs, type);  // tf

                String lastDocName = lastDocNames.get(type);
                if (lastDocName == null || !lastDocName.equals(item.docName)) {
                    // new doc
                    addOne(dfs, type);
                    lastDocNames.put(type, item.docName);
                }
            }
            last = item;
        }

        @Override
        public void close() throws IOException {
            if (last != null) {
                writeCounts();
            }
            writer.close();
        }

        private void addOne(HashMap<String, Long> map, String type) {
            long count = map.containsKey(type) ? map.get(type) : 0;
            map.put(type, count + 1);
        }

        private void writeCounts() throws IOException {
            // term tf df
            writer.write(last.term);
            for (String type : CandidateList.clistTypes) {
                long tf = tfs.containsKey(type) ? tfs.get(type) : 0;
                long df = dfs.containsKey(type) ? dfs.get(type) : 0;
                writer.write("\t" + tf + "\t" + df);
            }
            writer.newLine();
        }

    }
    
    
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfListItem", order = {"+docName", "+listType"})
    public static class CandidateListDocFreqMetaWriter implements Processor<TfListItem> {

        BufferedWriter writer;
        TfListItem last;
        HashMap<String, String> lastDocNames;
        HashMap<String, Long> ctfs; // collection tf freq
        HashMap<String, Long> cdfs; // collection doc freq

        public CandidateListDocFreqMetaWriter(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            String clistDfMetaFile = p.getString("clistDfMetaFile");
            writer = Utility.getWriter(clistDfMetaFile);
            lastDocNames = new HashMap<>();
            ctfs = new HashMap<>();
            cdfs = new HashMap<>();
        }

        @Override
        public void process(TfListItem item) throws IOException {
            for (String type : new String[]{"all", item.listType}) {
                addOne(ctfs, type);  // tf

                String lastDocName = lastDocNames.get(type);
                if (lastDocName == null || !lastDocName.equals(item.docName)) {
                    // new doc
                    addOne(cdfs, type);
                    lastDocNames.put(type, item.docName);
                }
            }
        }

        @Override
        public void close() throws IOException {
            writer.write("#term\t");
            writer.write(TextProcessing.join(CandidateList.clistTypes, "\t"));
            writer.newLine();
            if (last != null) {
                writeCounts();
            }
            writer.close();
        }

        private void addOne(HashMap<String, Long> map, String type) {
            long count = map.containsKey(type) ? map.get(type) : 0;
            map.put(type, count + 1);
        }

        private void writeCounts() throws IOException {
            // term tf df
            writer.write("COLLECTION");
            for (String type : CandidateList.clistTypes) {
                long tf = ctfs.containsKey(type) ? ctfs.get(type) : 0;
                long df = cdfs.containsKey(type) ? cdfs.get(type) : 0;
                writer.write("\t" + tf + "\t" + df);
            }
            writer.newLine();
        }

    }
}
