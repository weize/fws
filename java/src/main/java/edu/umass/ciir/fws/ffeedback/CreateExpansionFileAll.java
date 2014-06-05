/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.clustering.FacetModelParamGenerator;
import edu.umass.ciir.fws.eval.FfeedbackTimeEstimator;
import edu.umass.ciir.fws.eval.QueryFacetEvaluator;
import static edu.umass.ciir.fws.ffeedback.RunExpansionAll.ExpandQueryWithFeedbacks.maxFeedbackTime;
import edu.umass.ciir.fws.query.QueryTopicSubtopicMap;
import edu.umass.ciir.fws.types.TfFacetFeedbackParams;
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
public class CreateExpansionFileAll extends AppFunction {

    @Override
    public String getName() {
        return "create-expansion-file";
    }

    @Override
    public String getHelpString() {
        return "fws create-expansion-file --expansionModel config.json\n";
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

        job.connect("split", "process", ConnectionAssignmentType.Each);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("params", new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(SplitFeedbacks.class, parameter));
        stage.add(Utility.getSorter(new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder()));
        stage.add(new OutputStep("params"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("params", new TfFacetFeedbackParams.FacetSourceFacetParamsFeedbackSourceFeedbackParamsOrder());

        stage.add(new InputStep("params"));
        stage.add(new Step(CreateExpansionFile.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFacetFeedbackParams")
    public static class SplitFeedbacks extends StandardStep<FileName, TfFacetFeedbackParams> {

        Parameters p;

        public SplitFeedbacks(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();

        }

        @Override
        public void process(FileName file) throws IOException {
        }

        @Override
        public void close() throws IOException {
            FacetModelParamGenerator facetParamGen = new FacetModelParamGenerator(p);
            FeedbackParameterGenerator feedbackParamGen = new FeedbackParameterGenerator(p);
            List<String> facetSources = p.getAsList("facetSources");
            List<String> feedbackSources = p.getAsList("feedbackSources");

            for (String facetSource : facetSources) {
                List<String> facetParams = facetParamGen.getParams(facetSource);
                for (String facetParam : facetParams) {
                    for (String feedbackSource : feedbackSources) {
                        List<String> feedbackParams = feedbackParamGen.getParams(feedbackSource);
                        for (String feedbackParam : feedbackParams) {
                            processor.process(new TfFacetFeedbackParams(facetSource,
                                    facetParam, feedbackSource, feedbackParam));
                        }
                    }

                }
            }

            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFacetFeedbackParams")
    public static class CreateExpansionFile implements Processor<TfFacetFeedbackParams> {

        String allFeedbackDir;
        QueryTopicSubtopicMap queryMap;
        ExpansionDirectory expansionDir;
        String expansionModel;
        ExpansionIdMap expIdMap;

        public CreateExpansionFile(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            allFeedbackDir = p.getString("feedbackDir");
            expansionDir = new ExpansionDirectory(p);
            File selectionFile = new File(p.getString("subtopicSelectedIdFile"));
            File queryFile = new File(p.getString("queryFile"));
            queryMap = new QueryTopicSubtopicMap(selectionFile, queryFile);
            expansionModel = p.getString("expansionModel");
            loadExpIdMap();

        }

        @Override
        public void process(TfFacetFeedbackParams param) throws IOException {
            Utility.infoProcessing(param);

            File expansionFie = expansionDir.getExpansionFile(param, expansionModel);
            File feedbackFile = new File(Utility.getFeedbackFileName(allFeedbackDir, param));
            List<FacetFeedback> fdbkList = FacetFeedback.load(feedbackFile);

            Utility.infoOpen(expansionFie);
            Utility.createDirectoryForFile(expansionFie);
            BufferedWriter writer = Utility.getWriter(expansionFie);

            for (FacetFeedback ff : fdbkList) {
                String oriQuery = queryMap.getQuery(ff.qid);
                List<String> sidList = queryMap.getSidSet(ff.qid);
                // each time append a feedback term, and do expansion
                ArrayList<FeedbackTerm> selected = new ArrayList<>();
                for (FeedbackTerm term : ff.terms) {
                    selected.add(term);
                    String expansion = FacetFeedback.toExpansionString(selected);

                    FacetFeedback ffbk = FacetFeedback.parseFromExpansionString(expansion);
                    int time = FfeedbackTimeEstimator.time(ffbk);
                    if (time > maxFeedbackTime) {
                        continue;
                    }
                    // should be in the map
                    if (!expIdMap.contains(ff.qid, expansionModel, expansion)) {
                        throw new IOException("expansion id not found for " + expansion);
                    }

                    QueryExpansion qe = new QueryExpansion(ff.qid, oriQuery, expansionModel, expansion, expIdMap);
                    qe.expand();

                    // for each subtopic
                    for (String sid : sidList) {
                        QuerySubtopicExpansion qse = new QuerySubtopicExpansion(qe, sid);
                        writer.write(qse.toString());
                    }

                }
            }

            writer.close();
            Utility.infoWritten(expansionFie);
        }

        @Override
        public void close() throws IOException {
        }

        private void loadExpIdMap() throws IOException {
            expIdMap = new ExpansionIdMap();
            for (String qid : queryMap.getQidSet()) {
                File expIdFile = expansionDir.getExpansionIdFile(qid);
                expIdMap.load(expIdFile);
            }
        }
    }

}
