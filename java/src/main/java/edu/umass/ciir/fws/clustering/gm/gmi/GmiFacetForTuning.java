    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmi;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.gm.AppendFacetRankerParameter;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.tool.app.ProcessQueryApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.DirectoryUtility;
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
 * gmi facet for cross-validation. convert clustered items to facets for train queries in each
 * folders for different parameter combination. Will be used for tuning.
 *
 * @author wkong
 */
public class GmiFacetForTuning extends AppFunction {

    @Override
    public String getName() {
        return "gmi-facet-for-tuning";
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
        job.add(getClusterStage(parameters));

        job.connect("split", "cluster", ConnectionAssignmentType.Each);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("clusters", new TfQueryParameters.IdParametersOrder());

        List<String> inputFiles = parameter.getAsList("queryFile");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(new Step(SplitTuneRuns.class, parameter));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("clusters"));

        return stage;
    }

    private Stage getClusterStage(Parameters parameters) {
        Stage stage = new Stage("cluster");

        stage.addInput("clusters", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("clusters"));
        stage.add(new Step(GmiClusterToFacetConverter.class, parameters));
        return stage;
    }

    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SplitTuneRuns extends StandardStep<FileName, TfQueryParameters> {

        long numFolders;
        GmiParameterSettings gmiSettings;
        String trainDir;
        String gmiRunDir;
        boolean skipExisting;

        public SplitTuneRuns(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            numFolders = parameters.getJSON().getLong("cvFolderNum");
            gmiSettings = new GmiParameterSettings(p);
            trainDir = DirectoryUtility.getTrainDir(p.getString("gmDir"));
            skipExisting = p.get("skipExisting", false);
            gmiRunDir = DirectoryUtility.getModelRunDir(p.getString("facetRunDir"), "gmi");
        }

        @Override
        public void process(FileName object) throws IOException {
        }

        @Override
        public void close() throws IOException {

            List<ModelParameters> paramsList = gmiSettings.getFacetingSettings();
            for (int i = 1; i <= numFolders; i++) {
                String folderId = String.valueOf(i);
                String trainQuery = Utility.getFileName(trainDir, String.valueOf(i), "train.query");
                for (TfQuery query : QueryFileParser.loadQueryList(trainQuery)) {
                    for (ModelParameters params : paramsList) {
                        File clusterFile = new File(DirectoryUtility.getGmiFoldFacetFilename(gmiRunDir, folderId, query.id, params.toFilenameString()));
                        if (skipExisting && clusterFile.exists()) {
                            Utility.infoSkipExisting(clusterFile);
                        } else {
                            String folderIdOption = Utility.parametersToString(folderId, "tune");
                            processor.process(new TfQueryParameters(query.id, folderIdOption, params.toString()));
                        }
                    }
                }
            }
            processor.close();
        }
    }

}
