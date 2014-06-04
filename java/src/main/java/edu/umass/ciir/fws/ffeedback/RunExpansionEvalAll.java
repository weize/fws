/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.types.TfFacetFeedbackParams;
import edu.umass.ciir.fws.types.TfQueryExpansionSubtopic;
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
 *
 * @author wkong
 */
public class RunExpansionEvalAll extends AppFunction {

    @Override
    public String getName() {
        return "run-expansion-eval-all";
    }

    @Override
    public String getHelpString() {
        return "fws run-expansion-eval-all --expansionModel=";
    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitFeedbackStage(parameters));
        job.add(getSplitExpansionStage(parameters));
        job.add(getCmbExpansionStage(parameters));
        job.add(getProcessStage(parameters));

        job.connect("splitFeedbacks", "splitExpansions", ConnectionAssignmentType.Each);
        job.connect("splitExpansions", "cmbExpansions", ConnectionAssignmentType.Combined);
        job.connect("cmbExpansions", "process", ConnectionAssignmentType.Each);

        return job;
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Stage getSplitFeedbackStage(Parameters parameter) {
        Stage stage = new Stage("splitFeedbacks");

        stage.addOutput("feedbacks", new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(CreateExpansionFileAll.SplitFeedbacks.class, parameter));
        stage.add(Utility.getSorter(new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder()));
        stage.add(new OutputStep("feedbacks"));

        return stage;
    }

    private Stage getSplitExpansionStage(Parameters parameter) {
        Stage stage = new Stage("splitExpansions");

        stage.addInput("feedbacks", new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder());
        stage.addOutput("expansionSubtopics", new TfQueryExpansionSubtopic.QidModelExpIdSidOrder());

        stage.add(new InputStep("feedbacks"));
        stage.add(new Step(GetExpansions.class, parameter));
        stage.add(Utility.getSorter(new TfQueryExpansionSubtopic.QidModelExpIdSidOrder()));
        stage.add(new OutputStep("expansionSubtopics"));

        return stage;
    }

    private Stage getCmbExpansionStage(Parameters parameters) {
        Stage stage = new Stage("cmbExpansions");

        stage.addInput("expansionSubtopics", new TfQueryExpansionSubtopic.QidModelExpIdSidOrder());
        stage.addOutput("expansionSubtopics2", new TfQueryExpansionSubtopic.QidModelExpIdSidOrder());

        stage.add(new InputStep("expansionSubtopics"));
        stage.add(new Step(UniqueQueryExpansionSubtopic.class));
        stage.add(new Step(FilterExpansionSubtopic.class, parameters));
        stage.add(new OutputStep("expansionSubtopics2"));
        return stage;
    }
    
    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("expansionSubtopics2", new TfQueryExpansionSubtopic.QidModelExpIdSidOrder());

        stage.add(new InputStep("expansionSubtopics2"));
        stage.add(new Step(RunExpansionEvalOracleCandidate.Eval.class, parameters));
        stage.add(new Step(DoNonethingForExpansionSubtopic.class, parameters));
        return stage;
    }

    
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic")
    public static class DoNonethingForExpansionSubtopic implements Processor<TfQueryExpansionSubtopic> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void process(TfQueryExpansionSubtopic object) throws IOException {
        }
    }
    /**
     * Read expansion from expansion file, and emit expansion with each
     * subtopics that has qrel in splitSqrel.
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFacetFeedbackParams")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic")
    public static class GetExpansions extends StandardStep<TfFacetFeedbackParams, TfQueryExpansionSubtopic> {

        ExpansionDirectory expansionDir;
        String expansionModel;

        public GetExpansions(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            expansionDir = new ExpansionDirectory(p);
            expansionModel = p.getString("expansionModel");
        }

        @Override
        public void process(TfFacetFeedbackParams ffParam) throws IOException {
            Utility.infoProcessing(ffParam);

            File expansionFie = expansionDir.getExpansionFile(ffParam, expansionModel);
            List<QuerySubtopicExpansion> qses = QuerySubtopicExpansion.load(expansionFie, expansionModel);
            for (QuerySubtopicExpansion qse : qses) {
                processor.process(qse.toTfQueryExpansionSubtopic());
            }
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic", order = {"+qid", "+model", "+expId", "+sid"})
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic", order = {"+qid", "+model", "+expId", "+sid"})
    public static class UniqueQueryExpansionSubtopic extends StandardStep<TfQueryExpansionSubtopic, TfQueryExpansionSubtopic> {

        TfQueryExpansionSubtopic last = null;

        @Override
        public void process(TfQueryExpansionSubtopic qes) throws IOException {
            if (last == null) {
                last = qes;
            } else if (!qes.qid.equals(last.qid)
                    || !qes.model.equals(last.model)
                    || qes.expId != last.expId
                    || !qes.sid.equals(last.sid)) {
                processor.process(last);
                last = qes;
            }
        }

        @Override
        public void close() throws IOException {
            if (last != null) {
                processor.process(last);
            }
            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic", order = {"+qid", "+model", "+expId", "+sid"})
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic", order = {"+qid", "+model", "+expId", "+sid"})
    public static class FilterExpansionSubtopic extends StandardStep<TfQueryExpansionSubtopic, TfQueryExpansionSubtopic> {

        ExpansionIdMap expIdMap;
        ExpansionDirectory expansionDir;

        public FilterExpansionSubtopic(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            expansionDir = new ExpansionDirectory(p);
        }

        @Override
        public void process(TfQueryExpansionSubtopic qes) throws IOException {
            File evalFile = new File(Utility.getQExpSubtopicTevalFileName(expansionDir.evalDir, qes));
            if (evalFile.exists()) {
                System.err.println("exists results for " + evalFile.getAbsolutePath());
            } else {
                processor.process(qes);
            }
        }
    }
}
