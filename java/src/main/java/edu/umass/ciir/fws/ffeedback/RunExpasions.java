/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryExpansion;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.query.StructuredQuery;
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
public class RunExpasions extends AppFunction {

    @Override
    public String getName() {
        return "run-expansions";
    }

    @Override
    public String getHelpString() {
        return "fws " + getName() + " [parameters...]\n"
                + AppFunction.getTupleFlowParameterString();
    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getProcessStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);

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
        p.getList("input").add(new File(parameter.getString("expansionSource")).getAbsolutePath());

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(getExpansionGeneratorClass(parameter), parameter));
        stage.add(Utility.getSorter(new TfQueryExpansion.QidModelExpIdOrder()));
        stage.add(new OutputStep("expansions"));
        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("expansions", new TfQueryExpansion.QidModelExpIdOrder());

        stage.add(new InputStep("expansions"));
        stage.add(new Step(RunExpanedQuery.class, parameters));
        return stage;
    }

    private Class getExpansionGeneratorClass(Parameters parameter) {
        String model = parameter.getString("expansionModel");
        if (model.equals("sts")) {
            return ExpandQueryWithSingleFacetTerm.class;
        } else {
            return ExpandQueryWithFeedbacks.class;
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansion")
    public static class RunExpanedQuery implements Processor<TfQueryExpansion> {

        String runDir;
        Retrieval retrieval;
        Parameters p;

        public RunExpanedQuery(TupleFlowParameters parameters) throws Exception {
            p = parameters.getJSON();
            runDir = p.getString("expansionRunDir");
            retrieval = RetrievalFactory.instance(p);
        }

        @Override
        public void process(TfQueryExpansion qe) throws IOException {
            File outfile = new File(Utility.getExpansionRunFileName(runDir, qe));
            Utility.createDirectoryForFile(outfile);
            BufferedWriter writer = Utility.getWriter(outfile);

            String queryNumber = qe.qid;
            String queryText = qe.expanedQuery;
            System.err.println(queryNumber + "\t" + queryText);

            // parse and transform query into runnable form
            List<ScoredDocument> results = null;

            Node root = StructuredQuery.parse(queryText);
            Node transformed;
            try {
                transformed = retrieval.transformQuery(root, p);
                // run query
                results = retrieval.executeQuery(transformed, p).scoredDocuments;
            } catch (Exception ex) {
                Logger.getLogger(RunExpasions.class.getName()).log(Level.SEVERE, "error in running for "
                        + qe.toString(), ex);
            }

            // if we have some results -- print in to output stream
            if (!results.isEmpty()) {
                for (ScoredDocument sd : results) {
                    writer.write(sd.toTRECformat(queryNumber));
                    writer.newLine();
                }
            }
            writer.close();
            Utility.infoWritten(outfile);
        }

        @Override
        public void close() throws IOException {
        }
    }

}
