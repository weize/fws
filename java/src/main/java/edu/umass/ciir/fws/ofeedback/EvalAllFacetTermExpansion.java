/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ofeedback;

import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.eval.TrecEvaluator;
import edu.umass.ciir.fws.query.QuerySubtopic;
import edu.umass.ciir.fws.query.QueryTopic;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lemurproject.galago.core.retrieval.Retrieval;
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
 * @author wkong
 */
public class EvalAllFacetTermExpansion extends AppFunction {

    @Override
    public String getName() {
        return "eval-all-facet-term-expansion";
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

        stage.addOutput("queryParameters", new TfQueryParameters.IdParametersOrder());

        File expTermFile = new File(parameter.getString("oracleExpandedTerms"));
        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        p.getList("input").add(expTermFile.getAbsolutePath());

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(ExpendQueryWithFaceTerm.class, parameter));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("queryParameters"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("queryParameters", new TfQueryParameters.IdParametersOrder());
        stage.addOutput("queryParameters2", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("queryParameters"));
        stage.add(new Step(Eval.class, parameters));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("queryParameters2"));
        return stage;
    }

    private Stage getWriterStage(Parameters parameters) {
        Stage stage = new Stage("write");

        stage.addInput("queryParameters2", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("queryParameters2"));
        stage.add(new Step(CombineAllEval.class, parameters));

        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class CombineAllEval implements Processor<TfQueryParameters> {

        String evalDir;
        File outfile;
        BufferedWriter writer;
        boolean first;
        String[] metrics;

        public CombineAllEval(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            evalDir = p.getString("oracleExpansionEvalDir");
            outfile = new File(p.getString("oracleExpansionEvalFile"));
            writer = Utility.getWriter(outfile);
            first = true;
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            String[] params = Utility.splitParameters(queryParams.parameters);
            String sid = params[0];
            String fid = params[1];
            String tid = params[2];

            File tevalFile = new File(Utility.getOracleExpandTevalFileName(evalDir, queryParams.id, sid, fid + "-" + tid));
            TrecEvaluator evaluator = new TrecEvaluator(tevalFile);
            List<QueryMetrics> qms = evaluator.resultToQueryMetrics();
            if (first) {
                metrics = Arrays.copyOf(evaluator.metrics, evaluator.metrics.length);
                first = false;
                writer.write(evaluator.getHeader());
                writer.newLine();
            } else {
                assert metrics.length == evaluator.metrics.length : "number of metrics not match " + queryParams.toString();
                for (int i = 0; i < metrics.length; i++) {
                    assert metrics[i].equals(evaluator.metrics[i]) : "metrics not match " + queryParams.toString();
                }
            }

            for (QueryMetrics qm : qms) {
                if (!qm.qid.equals("all")) {
                    writer.write(String.format("%s-%s-%s-%s\t%s\n",
                            queryParams.id, sid, fid, tid, TextProcessing.join(qm.values, "\t")));
                }
            }
        }

        @Override
        public void close() throws IOException {
            writer.close();
            Utility.infoWritten(outfile);
        }
    }

    /**
     * generate parameters
     */
    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class ExpendQueryWithFaceTerm extends StandardStep<FileName, TfQueryParameters> {

        @Override
        public void process(FileName file) throws IOException {
            BufferedReader reader = Utility.getReader(file.filename);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t");
                String qid = fields[0];
                String query = fields[1];
                String fidTid = fields[2];
                String term = fields[3];
                String parameters = fidTid;
                processor.process(new TfQueryParameters(qid, query, parameters));

            }
            reader.close();
        }

        @Override
        public void close() throws IOException {
            processor.close();
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class Eval extends StandardStep<TfQueryParameters, TfQueryParameters> {

        String runDir;
        String evalDir;
        Retrieval retrieval;
        Parameters p;
        HashMap<String, QueryTopic> queryTopics;
        String sqrelDir;
        TrecEvaluator evaluator;

        public Eval(TupleFlowParameters parameters) throws Exception {
            p = parameters.getJSON();
            runDir = p.getString("oracleExpansionRunDir");
            evalDir = p.getString("oracleExpansionEvalDir");
            sqrelDir = p.getString("sqrelDir");
            File queryJsonFile = new File(p.getString("queryJsonFile"));
            queryTopics = QueryTopic.loadQueryFullTopicsAsMap(queryJsonFile);
            evaluator = new TrecEvaluator(p.getString("trecEval"));
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            String fidTid = queryParams.parameters;
            String fid = fidTid.split("-")[0];
            String tid = fidTid.split("-")[1];
            String rankFileName = Utility.getOracleExpandRunFileName(runDir, queryParams.id, fid, tid);
            for (QuerySubtopic qs : queryTopics.get(queryParams.id).subtopics) {
                File qrelFile = new File(Utility.getQrelForOneSubtopic(sqrelDir, queryParams.id, qs.sid));
                if (qrelFile.exists()) {
                    File tevalFile = new File(Utility.getOracleExpandTevalFileName(evalDir, queryParams.id, qs.sid, fidTid));
                    try {
                        evaluator.evalAndOutput(qrelFile.getAbsolutePath(), rankFileName, tevalFile);

                        String params = Utility.parametersToString(qs.sid, fid, tid);
                        processor.process(new TfQueryParameters(queryParams.id, queryParams.text, params));
                    } catch (Exception ex) {
                        Logger.getLogger(EvalAllFacetTermExpansion.class.getName()).log(Level.SEVERE, "error in eval " + queryParams.toString(), ex);
                    }

                }
            }
        }

        @Override
        public void close() throws IOException {
            processor.close();
        }

    }

}
