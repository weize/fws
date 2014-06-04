/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.clustering.FacetModelParamGenerator;
import edu.umass.ciir.fws.eval.FfeedbackTimeEstimator;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryExpansion;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
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
public class RunExpansionAll extends AppFunction {

    @Override
    public String getName() {
        return "run-expansion-all";
    }

    @Override
    public String getHelpString() {
        return "fws run-expansion-all --expansionModel=";
    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getSplitExpansionStage(parameters));
        job.add(getCmbExpansionStage(parameters));
        job.add(getProcessStage(parameters));

        job.connect("split", "splitExpansions", ConnectionAssignmentType.Each);
        job.connect("splitExpansions", "cmbExpansions", ConnectionAssignmentType.Combined);
        job.connect("cmbExpansions", "process", ConnectionAssignmentType.Each);

        return job;
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("queries", new TfQuery.IdOrder());

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        p.getList("input").add(new File(parameter.getString("queryFile")).getAbsolutePath());

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(QueryFileParser.class));
        stage.add(Utility.getSorter(new TfQuery.IdOrder()));
        stage.add(new OutputStep("queries"));

        return stage;
    }

    private Stage getSplitExpansionStage(Parameters parameters) {
        Stage stage = new Stage("splitExpansions");

        stage.addInput("queries", new TfQuery.IdOrder());
        stage.addOutput("expansions", new TfQueryExpansion.QidModelExpIdOrder());

        stage.add(new InputStep("queries"));
        stage.add(new Step(ExpandQueryWithFeedbacks.class, parameters));
        stage.add(Utility.getSorter(new TfQueryExpansion.QidModelExpIdOrder()));
        stage.add(new Step(UniqueQueryExpansion.class));
        stage.add(new OutputStep("expansions"));
        return stage;
    }

    private Stage getCmbExpansionStage(Parameters parameters) {
        Stage stage = new Stage("cmbExpansions");

        stage.addInput("expansions", new TfQueryExpansion.QidModelExpIdOrder());
        stage.addOutput("expansions2", new TfQueryExpansion.QidModelExpIdOrder());

        stage.add(new InputStep("expansions"));
        stage.add(new Step(UniqueQueryExpansion.class));
        stage.add(new Step(FilterExpansion.class, parameters));
        stage.add(new OutputStep("expansions2"));
        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("expansions2", new TfQueryExpansion.QidModelExpIdOrder());

        stage.add(new InputStep("expansions2"));
        stage.add(new Step(RunExpandedQuery.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansion")
    public static class ExpandQueryWithFeedbacks extends StandardStep<TfQuery, TfQueryExpansion> {

        FacetModelParamGenerator facetParamGen;
        FeedbackParameterGenerator feedbackParamGen;
        final static int  maxFeedbackTime = 50;

        String allFeedbackDir;
        ExpansionIdMap expIdMap;
        String expansionModel;
        ExpansionDirectory expansionDir;
        List<String> facetSources;
        List<String> feedbackSources;

        public ExpandQueryWithFeedbacks(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            // setup
            expansionModel = p.getString("expansionModel");
            facetParamGen = new FacetModelParamGenerator(p);
            feedbackParamGen = new FeedbackParameterGenerator(p);
            allFeedbackDir = p.getString("feedbackDir");
            expansionDir = new ExpansionDirectory(p);
            facetSources = p.getAsList("facetSources");
            feedbackSources = p.getAsList("feedbackSources");
        }

        @Override
        public void process(TfQuery query) throws IOException {
            Utility.infoProcessing(query);
            File expIdFile = expansionDir.getExpansionIdFile(query.id);
            if (expIdFile.exists()) {
                expIdMap = new ExpansionIdMap(expIdFile);
            } else {
                expIdMap = new ExpansionIdMap();
            }

            for (String facetSource : facetSources) {
                List<String> facetParams = facetParamGen.getParams(facetSource);
                for (String facetParam : facetParams) {
                    for (String feedbackSource : feedbackSources) {
                        List<String> feedbackParams = feedbackParamGen.getParams(feedbackSource);
                        for (String feedbackParam : feedbackParams) {
                            processAndEmit(query, facetSource, facetParam, feedbackSource, feedbackParam);
                        }
                    }

                }
            }

            Utility.infoOpen(expIdFile);
            expIdMap.output(expIdFile); // update ids
            Utility.infoWritten(expIdFile);
        }

        private void processAndEmit(TfQuery query, String facetSource, String facetParam, String feedbackSource, String feedbackParam) throws IOException {
            File feedbackFile = new File(Utility.getFeedbackFileName(allFeedbackDir, facetSource, facetParam, feedbackSource, feedbackParam));
            Utility.infoProcessing(feedbackFile);
            List<FacetFeedback> fdbkList = FacetFeedback.load(feedbackFile);

            for (FacetFeedback ff : fdbkList) {
                if (ff.qid.equals(query.id)) {
                    String oriQuery = query.text;
                    // each time append a feedback term, and do expansion
                    ArrayList<FeedbackTerm> selected = new ArrayList<>();
                    for (FeedbackTerm term : ff.terms) {
                        selected.add(term);
                        String expansion = FacetFeedback.toExpansionString(selected);

                        FacetFeedback ffbk = FacetFeedback.parseFromExpansionString(expansion);
                        int time = FfeedbackTimeEstimator.time(ffbk);
                        if (time <= maxFeedbackTime) {
                            QueryExpansion qe = new QueryExpansion(query.id, oriQuery, expansionModel, expansion, expIdMap);
                            qe.expand();
                            processor.process(qe.toTfQueryExpansion());
                        }
                    }
                }
            }
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansion", order = {"+qid", "+model", "+expId"})
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansion", order = {"+qid", "+model", "+expId"})
    public static class FilterExpansion extends StandardStep<TfQueryExpansion, TfQueryExpansion> {

        ExpansionDirectory expansionDir;
        long count;
        BufferedWriter writer;

        public FilterExpansion(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            expansionDir = new ExpansionDirectory(p);
            writer = Utility.getWriter(new File(expansionDir.getRunListFile()));
            count = 0;
        }

        @Override
        public void process(TfQueryExpansion qe) throws IOException {
            File runFile = new File(Utility.getExpansionRunFileName(expansionDir.runDir, qe));
            if (runFile.exists()) {
                System.err.println("exists results for " + runFile.getAbsolutePath());
            } else {
                processor.process(qe);
                writer.write(String.format("%s\t%s\n", QueryExpansion.toId(qe), qe.expanedQuery));
                count++;
            }
        }

        @Override
        public void close() throws IOException {
            processor.close();
            writer.close();
            System.err.println("Submit " + count + " runs");
        }
    }
}
