/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.clustering.FacetModelParamGenerator;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
import edu.umass.ciir.fws.types.TfQueryExpansionSubtopic;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 *
 * Evaluate expansion runs with respect to each subtopics.
 *
 * @author wkong
 */
public class RunExpansionEvalAllNotUsed extends AppFunction {

    @Override
    public String getName() {
        return "xxxxxxxxxxxx";
    }

    @Override
    public String getHelpString() {
        return "fws " + getName() + " [parameters...]\n"
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
        job.add(getWriterStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);
        job.connect("process", "write", ConnectionAssignmentType.Combined);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("expansionSubtopics", new TfQueryExpansionSubtopic.QidModelExpIdSidOrder());

        File expFile = new File(parameter.getString("queryFile"));
        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        p.getList("input").add(expFile.getAbsolutePath());

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(GetQExpansionSubtopics.class, parameter));
        stage.add(Utility.getSorter(new TfQueryExpansionSubtopic.QidModelExpIdSidOrder()));
        stage.add(new OutputStep("expansionSubtopics"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("expansionSubtopics", new TfQueryExpansionSubtopic.QidModelExpIdSidOrder());
        stage.addOutput("expansionSubtopics2", new TfQueryExpansionSubtopic.QidModelExpIdSidOrder());

        stage.add(new InputStep("expansionSubtopics"));
        stage.add(new Step(Eval.class, parameters));
        stage.add(Utility.getSorter(new TfQueryExpansionSubtopic.QidModelExpIdSidOrder()));
        stage.add(new OutputStep("expansionSubtopics2"));
        return stage;
    }

    private Stage getWriterStage(Parameters parameters) {
        Stage stage = new Stage("write");

        stage.addInput("expansionSubtopics2", new TfQueryExpansionSubtopic.QidModelExpIdSidOrder());

        stage.add(new InputStep("expansionSubtopics2"));
        stage.add(new Step(CombineAllEval.class, parameters));

        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic")
    public static class Eval extends StandardStep<TfQueryExpansionSubtopic, TfQueryExpansionSubtopic> {

        String runDir;
        String evalDir;
        String sqrelDir;
        TrecEvaluator evaluator;

        public Eval(TupleFlowParameters parameters) throws Exception {
            Parameters p = parameters.getJSON();
            runDir = p.getString("expansionRunDir");
            evalDir = p.getString("expansionEvalDir");
            sqrelDir = p.getString("sqrelSplitDir");
            evaluator = new TrecEvaluator(p.getString("trecEval"));
        }

        @Override
        public void process(TfQueryExpansionSubtopic qes) throws IOException {
            String rankFileName = Utility.getExpansionRunFileName(runDir, qes);
            String qrelFileName = Utility.getQrelForOneSubtopic(sqrelDir, qes.qid, qes.sid);
            File tevalFile = new File(Utility.getQExpSubtopicTevalFileName(evalDir, qes));
            try {
                if (tevalFile.exists()) {
                    System.err.println("exsits " + tevalFile.getAbsolutePath());
                } else {
                    Utility.createDirectoryForFile(tevalFile);
                    evaluator.evalAndOutput(qrelFileName, rankFileName, tevalFile);
                    Utility.infoWritten(tevalFile);
                }
                processor.process(qes);
            } catch (Exception ex) {
                Logger.getLogger(RunExpansionEvalAllNotUsed.class.getName()).log(Level.SEVERE, "error in eval " + qes.toString(), ex);
            }

        }

        @Override
        public void close() throws IOException {
            processor.close();
        }

    }

    /**
     * Read expansion from expansion file, and emit expansion with each
     * subtopics that has qrel in splitSqrel.
     */
    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic")
    public static class GetQExpansionSubtopics extends StandardStep<FileName, TfQueryExpansionSubtopic> {

        String model;
        Parameters p;

        public GetQExpansionSubtopics(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();

        }

        @Override
        public void process(FileName file) throws IOException {

        }

        @Override
        public void close() throws IOException {
            model = p.getString("expansionModel");
            FacetModelParamGenerator modelParams = new FacetModelParamGenerator(p);

            List<String> expansionSources = p.getAsList("expansionSources");

            for (String source : expansionSources) {
                List<String> params = modelParams.getParams(source);
                for (String param : params) {
                    processAndEmit(source, param);

                }
            }

            processor.close();
        }

        private void processAndEmit(String source, String param) {
//            List<QuerySubtopicExpansion> qses = QuerySubtopicExpansion.load(new File(file.filename), model);
//            for (QuerySubtopicExpansion qse : qses) {
//                File qrelFile = new File(Utility.getQrelForOneSubtopic(sqrelDir, qse.qid, qse.sid));
//                if (qrelFile.exists()) {
//                    processor.process(qse.toTfQueryExpansionSubtopic());
//                }
//            }
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic")
    public static class CombineAllEval implements Processor<TfQueryExpansionSubtopic> {

        String evalDir;
        File outfile;
        BufferedWriter writer;
        boolean first;
        String[] metrics;

        public CombineAllEval(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            evalDir = p.getString("expansionEvalDir");
            outfile = new File(p.getString("expansionEvalFile"));
            writer = Utility.getWriter(outfile);
            first = true;
        }

        @Override
        public void process(TfQueryExpansionSubtopic qes) throws IOException {
            File tevalFile = new File(Utility.getQExpSubtopicTevalFileName(evalDir, qes));
            TrecEvaluator evaluator = new TrecEvaluator(tevalFile);
            List<QueryMetrics> qms = evaluator.resultToQueryMetrics();

            // verify metrics are consistent across different evaluation files
            if (first) {
                metrics = Arrays.copyOf(evaluator.metrics, evaluator.metrics.length);
                first = false;
                writer.write(evaluator.getHeader());
                writer.newLine();
            } else {
                assert metrics.length == evaluator.metrics.length : "number of metrics not match " + tevalFile.getAbsolutePath();
                for (int i = 0; i < metrics.length; i++) {
                    assert metrics[i].equals(evaluator.metrics[i]) : "metrics not match " + tevalFile.getAbsolutePath();
                }
            }

            for (QueryMetrics qm : qms) {
                if (!qm.qid.equals("all")) {
                    qm.qid = QuerySubtopicExpansion.toId(qes);
                    writer.write(qm.toString());
                    writer.newLine();
                }
            }
        }

        @Override
        public void close() throws IOException {
            writer.close();
            Utility.infoWritten(outfile);
        }
    }

}
