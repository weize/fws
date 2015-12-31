    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmi;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.gm.AppendFacetRankerParameter;
import edu.umass.ciir.fws.clustering.gm.ExtractTermPairDataForPrediectedTerms;
import edu.umass.ciir.fws.clustering.gm.PairPredictor;
import edu.umass.ciir.fws.clustering.gm.TermPredictor;
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
 * predict term, pair probability for tuning
 *
 * @author wkong
 */
public class GmiPredictForTuning extends AppFunction {

    @Override
    public String getName() {
        return "gmi-predict-for-tuning";
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
        job.add(getPredictStage(parameters));

        job.connect("split", "predict", ConnectionAssignmentType.Each);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("queryParams", new TfQueryParameters.IdParametersOrder());

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

    private Stage getPredictStage(Parameters parameters) {
        Stage stage = new Stage("predict");

        stage.addInput("queryParams", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("queryParams"));
        stage.add(new Step(TermPredictorForTuning.class, parameters));
        stage.add(new Step(ExtractTermPairDataForPrediectedTermsForTuning.class, parameters));
        stage.add(new Step(PairPredictorForTuning.class, parameters));
        stage.add(new Step(ProcessQueryApp.DoNonethingForQueryParams.class));

        return stage;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SplitTuneRuns extends StandardStep<FileName, TfQueryParameters> {

        long numFolders;
        String trainDir;

        public SplitTuneRuns(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            String gmDir = p.getString("gmDir");
            trainDir = Utility.getFileName(gmDir, "train");
            numFolders = parameters.getJSON().getLong("cvFolderNum");
        }

        @Override
        public void process(FileName filename) throws IOException {
        }

        @Override
        public void close() throws IOException {
            for (int i = 1; i <= numFolders; i++) {
                String folderId = String.valueOf(i);
                String trainQuery = Utility.getFileName(trainDir, folderId, "train.query");
                for (TfQuery query : QueryFileParser.loadQueryList(trainQuery)) {
                    processor.process(new TfQueryParameters(query.id, query.text, folderId));
                }
            }
            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class TermPredictorForTuning extends TermPredictor {

        String gmiRunDir;

        public TermPredictorForTuning(TupleFlowParameters parameters) {
            super(parameters);
            String facetRunDir = parameters.getJSON().getString("gmDir");
            gmiRunDir = DirectoryUtility.getModelRunDir(facetRunDir, "gmi");
        }

        @Override
        public String getPredictBaseDir(String foldId) {
            return Utility.getFileName(gmiRunDir, foldId, "predict");
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class ExtractTermPairDataForPrediectedTermsForTuning extends ExtractTermPairDataForPrediectedTerms {

        String gmiRunDir;

        public ExtractTermPairDataForPrediectedTermsForTuning(TupleFlowParameters parameters) throws Exception {
            super(parameters);
            String facetRunDir = parameters.getJSON().getString("gmDir");
            gmiRunDir = DirectoryUtility.getModelRunDir(facetRunDir, "gmi");
        }

        @Override
        public String getPredictBaseDir(String foldId) {
            return Utility.getFileName(gmiRunDir, foldId, "predict");
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class PairPredictorForTuning extends PairPredictor {

        String gmiRunDir;

        public PairPredictorForTuning(TupleFlowParameters parameters) {
            super(parameters);
            String facetRunDir = parameters.getJSON().getString("gmDir");
            gmiRunDir = DirectoryUtility.getModelRunDir(facetRunDir, "gmi");
        }

        @Override
        public String getPredictBaseDir(String foldId) {
            return Utility.getFileName(gmiRunDir, foldId, "predict");
        }
    }

}
