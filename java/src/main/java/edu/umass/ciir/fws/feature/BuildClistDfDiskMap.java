/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import static edu.umass.ciir.fws.feature.CandidateListDocFreqMap.size;
import edu.umass.ciir.fws.types.TfListItem;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.btree.simple.DiskMapSortedBuilder;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.core.types.KeyValuePair;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
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
 *
 * @author wkong
 */
public class BuildClistDfDiskMap extends AppFunction {

    @Override
    public String getName() {
        return "build-clistdf-map";
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Stage getSplitStage(Parameters parameters) {
        Stage stage = new Stage("split");

        stage.addOutput("fileNames", new FileName.FilenameOrder());

        List<String> inputFiles = parameters.getAsList("input");

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

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getProcessStage(parameters));
        job.add(getWriteStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);
        job.connect("process", "write", ConnectionAssignmentType.Combined);

        return job;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("fileNames", new FileName.FilenameOrder());
        stage.addOutput("KeyValuePairs", new KeyValuePair.KeyOrder());

        stage.add(new InputStep("fileNames"));
        stage.add(new Step(ParseFileToKeyValue.class, parameters));
        stage.add(Utility.getSorter(new KeyValuePair.KeyOrder()));
        stage.add(new OutputStep("KeyValuePairs"));
        return stage;
    }

    private Stage getWriteStage(Parameters parameters) {
        Stage stage = new Stage("write");

        stage.addInput("KeyValuePairs", new KeyValuePair.KeyOrder());

        stage.add(new InputStep("KeyValuePairs"));
        stage.add(new Step(MyDiskMapSortedBuilder.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "org.lemurproject.galago.core.types.KeyValuePair")
    public static class ParseFileToKeyValue extends StandardStep<FileName, KeyValuePair> {

        public ParseFileToKeyValue() {
        }

        @Override
        public void process(FileName file) throws IOException {
            BufferedReader reader = Utility.getReader(file.filename);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] elems = line.split("\t");
                String term = elems[0];
                long[] curDFs = new long[size];
                for (int i = 1; i < elems.length; i++) {
                    curDFs[i - 1] = Integer.parseInt(elems[i]);
                }

                processor.process(new KeyValuePair(Utility.convertToBytes(term), Utility.convertToBytes(curDFs)));
            }

            reader.close();

        }
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.core.types.KeyValuePair", order = {"+key"})
    public static class MyDiskMapSortedBuilder extends DiskMapSortedBuilder {

        public MyDiskMapSortedBuilder(String path, Parameters opts) throws IOException {
            super(path, opts);
        }

        public MyDiskMapSortedBuilder(TupleFlowParameters parameters) throws IOException {
            super(parameters.getJSON().getString("output"), parameters.getJSON());

        }

    }

}
