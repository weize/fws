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
import edu.umass.ciir.fws.query.QueryTopicSubtopicMap;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryExpansion;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
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
 * Not finished
 *
 * @author wkong
 */
public class RunExpasionsFromList extends AppFunction {

    @Override
    public String getName() {
        return "run-expansion-from-list";
    }

    @Override
    public String getHelpString() {
        return "fws run-expansion-from-list --idList=<>";
    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
//        job.add(getSplitExpansionStage(parameters));
//        job.add(getCmbExpansionStage(parameters));
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

        stage.addOutput("expansions", new TfQueryExpansion.QidModelExpIdOrder());

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        p.getList("input").add(new File(parameter.getString("queryFile")).getAbsolutePath());

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(SplitExpansion.class));
        stage.add(Utility.getSorter(new TfQuery.IdOrder()));
        stage.add(new OutputStep("expansions"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("expansions", new TfQueryExpansion.QidModelExpIdOrder());

        stage.add(new InputStep("expansions"));
        stage.add(new Step(RunExpandedQuery.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansion")
    public class SplitExpansion extends StandardStep<FileName, TfQueryExpansion> {

        ExpansionIdMap map;
        ExpansionDirectory expansionDir;
        String lastQid;

        public SplitExpansion(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            expansionDir = new ExpansionDirectory(p);
            map = new ExpansionIdMap();
        }

        @Override
        public void process(FileName fileName) throws IOException {
            BufferedReader reader = Utility.getReader(fileName.filename);
            String line;
            while ((line = reader.readLine()) != null) {
                QueryExpansion qe = getQueryExpansion(line);
                processor.process(qe.toTfQueryExpansion());
            }
            reader.close();

        }

        private QueryExpansion getQueryExpansion(String line) throws IOException {

            String[] elems = line.split("\t");
            String qid = elems[0];
            String model = elems[1];
            String expId = elems[2];

            if (lastQid == null || !lastQid.equals(qid)) {
                File file = expansionDir.getExpansionIdFile(qid);
                map.load(file);
                lastQid = qid;
            }

//            String expansion = map
            return null;

        }
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
        final static int topFacetNum = 15;

        public ExpandQueryWithSingleFacetTerm(TupleFlowParameters parameters) throws IOException {
            p = parameters.getJSON();
            // setup
            expansionModel = p.getString("expansionModel");
            modelParams = new FacetModelParamGenerator(p);
            allFacetDir = p.getString("facetDir");
            expansionDir = new ExpansionDirectory(p);
            facetSources = p.getAsList("facetSources");
        }

        @Override
        public void process(TfQuery query) throws IOException {
            File expIdFile = expansionDir.getExpansionIdFile(query.id);
            if (expIdFile.exists()) {
                expIdMap = new ExpansionIdMap(expIdFile);
            } else {
                expIdMap = new ExpansionIdMap();
            }

            for (String source : facetSources) {
                List<String> params = modelParams.getParams(source);
                for (String param : params) {
                    processAndEmit(source, query, param);

                }
            }

            Utility.infoOpen(expIdFile);
            expIdMap.output(expIdFile); // update ids
            Utility.infoWritten(expIdFile);
        }

        @Override
        public void close() throws IOException {
            // closing
            processor.close();
        }

        private void processAndEmit(String source, TfQuery query, String param) throws IOException {
            String facetDir = Utility.getFileName(allFacetDir, source, "facet");
            File facetFile = new File(Utility.getFacetFileName(facetDir, query.id, source, param));
            List<ScoredFacet> facets = ScoredFacet.loadFacets(facetFile);

            // only use top facets
            for (ScoredFacet facet : facets.subList(0, Math.min(facets.size(), topFacetNum))) {
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
        long count;

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
            count = 0;

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
                count++;
            }
        }

        @Override
        public void close() throws IOException {
            // closing
            processor.close();
            writer.close();
            Utility.infoWritten(expFile);
            System.err.println("Submit " + count + " runs");

        }
    }
}
