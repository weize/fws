/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.clustering.FacetModelParamGenerator;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.query.QueryTopicSubtopicMap;
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
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 *
 * @author wkong
 */
public class RunOracleCandidateExpasions extends AppFunction {

    @Override
    public String getName() {
        return "run-oracle-candidate-expansions";
    }

    @Override
    public String getHelpString() {
        return "fws run-oracle-candidate-expansions --expansionModel=";
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

        stage.addInput("queries", new TfQuery.IdOrder());
        stage.add(new Step(ExpandQueryWithSingleFacetTerm.class, parameters));
        stage.add(Utility.getSorter(new TfQueryExpansion.QidModelExpIdOrder()));
        stage.add(new OutputStep("expansions"));
        return stage;
    }

    private Stage getCmbExpansionStage(Parameters parameters) {
        Stage stage = new Stage("cmbExpansions");

        stage.addInput("expansions", new TfQueryExpansion.QidModelExpIdOrder());
        stage.addOutput("expansions2", new TfQueryExpansion.QidModelExpIdOrder());

        stage.addInput("expansions", new TfQueryExpansion.QidModelExpIdOrder());
        stage.add(new Step(UniqueQueryExpansion.class));
        stage.add(new Step(WriteExpansionFile.class, parameters));
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
    public static class ExpandQueryWithSingleFacetTerm extends StandardStep<TfQuery, TfQueryExpansion> {

        FacetModelParamGenerator modelParams;

        String allFacetDir;
        ExpansionIdMap expIdMap;
        String expansionModel;
        Parameters p;
        ExpansionDirectory expansionDir;
        List<String> facetSources;

        public ExpandQueryWithSingleFacetTerm(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
            // setup
            expansionModel = p.getString("expansionModel");
            modelParams = new FacetModelParamGenerator(p);
            allFacetDir = p.getString("facetDir");
            expansionDir = new ExpansionDirectory(p);
            if (expansionDir.expansionIdFile.exists()) {
                expIdMap = new ExpansionIdMap(expansionDir.expansionIdFile);
            } else {
                expIdMap = new ExpansionIdMap();
            }
            facetSources = p.getAsList("facetSources");
        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (String source : facetSources) {
                List<String> params = modelParams.getParams(source);
                for (String param : params) {
                    processAndEmit(source, query, param);

                }
            }
        }

        @Override
        public void close() throws IOException {
            // closing
            processor.close();
            Utility.infoOpen(expansionDir.expansionIdFile);
            expIdMap.output(expansionDir.expansionIdFile); // update ids
            Utility.infoWritten(expansionDir.expansionIdFile);

        }

        private void processAndEmit(String source, TfQuery query, String param) throws IOException {
            String facetDir = Utility.getFileName(allFacetDir, source, "facet");
            File facetFile = new File(Utility.getFacetFileName(facetDir, query.id, source, param));
            List<ScoredFacet> facets = ScoredFacet.loadFacets(facetFile);

            // only use top 30
            for (ScoredFacet facet : facets.subList(0, Math.min(facets.size(), 30))) {
                String oriQuery = query.text;

                int fidx = 0;
                int tidx = 0;

                for (ScoredItem item : facet.items) {
                    FeedbackTerm ft = new FeedbackTerm(item.item, fidx, tidx);
                    QueryExpansion qe = new QueryExpansion(query.id, oriQuery, expansionModel, ft.toString(), expIdMap);
                    qe.expand();
                    processor.process(qe.toTfQueryExpansion());
                }
            }

        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansion", order = {"+qid", "+model", "+expId"})
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansion", order = {"+qid", "+model", "+expId"})
    public static class WriteExpansionFile extends StandardStep<TfQueryExpansion, TfQueryExpansion> {

        QueryTopicSubtopicMap queryMap;

        File expFile; // expansion file for kee tracke of qid and its expaions
        BufferedWriter writer;
        ExpansionIdMap expIdMap;
        String expansionModel;
        ExpansionDirectory expansionDir;

        public WriteExpansionFile(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            expansionDir = new ExpansionDirectory(p);
            expansionModel = p.getString("expansionModel");
            File selectionFile = new File(p.getString("subtopicSelectedIdFile"));
            File queryFile = new File(p.getString("queryFile"));
            queryMap = new QueryTopicSubtopicMap(selectionFile, queryFile);
            expFile = expansionDir.getExpansionFile("oracle", expansionModel);
            Utility.infoOpen(expFile);
            writer = Utility.getWriter(expFile);

        }

        @Override
        public void process(TfQueryExpansion qe) throws IOException {
            List<String> sidList = queryMap.getSidSet(qe.qid);
            for (String sid : sidList) {
                QuerySubtopicExpansion qse = new QuerySubtopicExpansion(qe, sid);
                writer.write(qse.toString());
            }

            File runFile = new File(Utility.getExpansionRunFileName(expansionDir.runDir, qe));
            if (runFile.exists()) {
                System.err.println("exists results for " + runFile.getAbsolutePath());
            } else {
                processor.process(qe);
            }
        }

        @Override
        public void close() throws IOException {
            // closing
            processor.close();
            writer.close();
            Utility.infoWritten(expFile);

        }
    }
}
