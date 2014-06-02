/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.Job;

/**
 *
 * @author wkong
 */
public class ExtractSimulatedFfeedback extends AppFunction {

    @Override
    public String getName() {
        return "extract-simulated-ffeedback";
    }

    @Override
    public String getHelpString() {
        return "fws extract-simulated-ffeedback --qfModel=<queryFacetModel> --qfParamStr=<param> config.json\n";
    }

    
//     @Override
//    public void run(Parameters p, PrintStream output) throws Exception {
//        Job job = createJob(p);
//        AppFunction.runTupleFlowJob(job, p, output);
//
//    }
//
//    private Job createJob(Parameters parameters) {
//        Job job = new Job();
//
//        job.add(getSplitStage(parameters));
//        job.add(getProcessStage(parameters));
//        
//        job.connect("split", "process", ConnectionAssignmentType.Each);
//
//        return job;
//    }

    
    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        String qfModel = p.getString("qfModel"); // qd, plsa, lda, gmi, gmj...
        String expDir = p.getString("expDir");
        String facetDir = Utility.getFileName(expDir, qfModel, "facet");
        String qfParamStr = p.get("qfParamStr", "");
        String feedbackDir = p.getString("feedbackDir");
        File outfile = new File(Utility.getFileNameWithSuffix(feedbackDir, qfModel, qfModel + "-" + qfParamStr, "fdbk"));
        File annotatorFdbkFile = new File(p.getString("annotatorFeedback"));

        BufferedWriter writer = Utility.getWriter(outfile);
        List<FacetFeedback> anntatorFkList = FacetFeedback.load(annotatorFdbkFile);
        for (FacetFeedback anFk : anntatorFkList) {
            File facetFile = new File(Utility.getFacetFileName(facetDir, anFk.qid, qfModel, qfParamStr));
            List<ScoredFacet> facets = ScoredFacet.loadFacets(facetFile);
            FacetFeedback simulatedFdbk = FacetFeedback.getSimulatedFfeedback(anFk, facets);
            writer.write(simulatedFdbk.toString());
            writer.newLine();
        }

        writer.close();
        Utility.infoWritten(outfile);

    }

}
