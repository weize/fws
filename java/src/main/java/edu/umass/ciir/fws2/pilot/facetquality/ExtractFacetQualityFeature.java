/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws2.pilot.facetquality;

import edu.umass.ciir.fws2.pilot.qperformance.*;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * extract *quality* and features for single facet
 * @author wkong
 */
public class ExtractFacetQualityFeature extends AppFunction {
    @Override
    public String getName() {
            return "extract-facet-quality-feature";
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        prepareDir(p);
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);
    }

    private Job createJob(Parameters p) {
        Job job = new Job();
       
        job.add(getSplitQueriesStage(p));
        job.add(getExtractFeaturesStage(p));
        
        job.connect("splitQueries", "extractFeatures", ConnectionAssignmentType.Each);        
        
        return job;
        
    }

    private Stage getSplitQueriesStage(Parameters parameter) {
        Stage stage = new Stage("splitQueries");

        stage.addOutput("queries", new TfQuery.IdOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(Utility.getSorter(new FileName.FilenameOrder()));
        stage.add(new Step(QueryFileParser.class));
        stage.add(Utility.getSorter(new TfQuery.IdOrder()));
        stage.add(new OutputStep("queries"));

        return stage;
    }

    private Stage getExtractFeaturesStage(Parameters p) {
        Stage stage = new Stage("extractFeatures");
        
        stage.addInput("queries", new TfQuery.IdOrder());
        
        stage.add(new InputStep("queries"));
        stage.add(new Step(FacetQualityFeaturesExtractor.class, p));
        
        return stage;
    }

    private void prepareDir(Parameters p) {
        String facetQualityDir = p.getString("fqDir");
        String featureDir = Utility.getFileName(facetQualityDir, "feature");
        Utility.createDirectory(featureDir);
    }
    
    
    
    
}
