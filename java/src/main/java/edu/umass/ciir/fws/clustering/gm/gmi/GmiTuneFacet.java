    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmi;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.TuneFacetModel;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiClusterParameters;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiFacetParameters;
import edu.umass.ciir.fws.eval.QueryMetrics;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfFolderParameters;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.ExNihiloSource;
import org.lemurproject.galago.tupleflow.IncompatibleProcessorException;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Linkage;
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
 * tune gmi for different evaluation metrics
 *
 * @author wkong
 */
public class GmiTuneFacet extends AppFunction {

    @Override
    public String getName() {
        return "gmi-tune-facet";
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

        job.add(getSplitEvalStage(parameters));
        job.add(getEvalStage(parameters));
        job.add(getSplitFoldStage(parameters));
        job.add(getSelectStage(parameters));
        job.add(getSplitFoldQueriesStage(parameters)); //splitFoldQueries
        job.add(getProcessStage(parameters));
        job.add(getWriteStage(parameters));

        job.connect("splitEval", "eval", ConnectionAssignmentType.Each);
        job.connect("eval", "splitFold", ConnectionAssignmentType.Combined);
        job.connect("splitFold", "select", ConnectionAssignmentType.Each);
        job.connect("select", "splitFoldQueries", ConnectionAssignmentType.Each);
        job.connect("splitFoldQueries", "process", ConnectionAssignmentType.Each);
        job.connect("select", "write", ConnectionAssignmentType.Combined);

        return job;
    }

    private Stage getSplitEvalStage(Parameters parameters) {
        Stage stage = new Stage("splitEval");

        stage.addOutput("folderParams", new TfFolderParameters.IdOptionParametersOrder());

        stage.add(new Step(SplitFoldersForTuneEval.class, parameters));
        stage.add(Utility.getSorter(new TfFolderParameters.IdOptionParametersOrder()));
        stage.add(new OutputStep("folderParams"));

        return stage;
    }

    private Stage getEvalStage(Parameters parameters) {
        Stage stage = new Stage("eval");

        stage.addInput("folderParams", new TfFolderParameters.IdOptionParametersOrder());
        stage.addOutput("folderParams2", new TfFolderParameters.IdOptionParametersOrder());

        stage.add(new InputStep("folderParams"));
        stage.add(new Step(EvalTuneGmi.class, parameters));
        stage.add(Utility.getSorter(new TfFolderParameters.IdOptionParametersOrder()));
        stage.add(new OutputStep("folderParams2"));
        return stage;
    }

    private Stage getSplitFoldStage(Parameters parameter) {
        Stage stage = new Stage("splitFold");

        stage.addInput("folderParams2", new TfFolderParameters.IdOptionParametersOrder());
        stage.addOutput("folderMetrics", new TfFolderParameters.IdOptionParametersOrder());

        stage.add(new InputStep("folderParams2"));
        stage.add(new Step(TuneFacetModel.SplitFoldMetrics.class, parameter));
        stage.add(new Step(SplitFoldMetricsAddRanker.class, parameter));
        stage.add(Utility.getSorter(new TfFolderParameters.IdOptionParametersOrder()));
        stage.add(new OutputStep("folderMetrics"));

        return stage;
    }

    private Stage getSelectStage(Parameters parameter) {
        Stage stage = new Stage("select");

        stage.addInput("folderMetrics", new TfFolderParameters.IdOptionParametersOrder());
        stage.addOutput("folderBestParams", new TfFolderParameters.IdOptionParametersOrder());

        stage.add(new InputStep("folderMetrics"));
        stage.add(new Step(SelectBestParam.class, parameter));
        stage.add(Utility.getSorter(new TfFolderParameters.IdOptionParametersOrder()));
        stage.add(new OutputStep("folderBestParams"));

        return stage;
    }

    private Stage getSplitFoldQueriesStage(Parameters parameters) {
        Stage stage = new Stage("splitFoldQueries");

        stage.addInput("folderBestParams", new TfFolderParameters.IdOptionParametersOrder());
        stage.addOutput("selectedQueryParams", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("folderBestParams"));
        stage.add(new Step(SplitFoldQueries.class, parameters));
        stage.add(Utility.getSorter(new TfQueryParameters.IdParametersOrder()));
        stage.add(new OutputStep("selectedQueryParams"));
        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("selectedQueryParams", new TfQueryParameters.IdParametersOrder());

        stage.add(new InputStep("selectedQueryParams"));
        stage.add(new Step(GmiClusterer.class, parameters));
        stage.add(new Step(GmiClusterToFacetConverter.class, parameters));

        return stage;
    }

    private Stage getWriteStage(Parameters parameters) {
        Stage stage = new Stage("write");

        stage.addInput("folderBestParams", new TfFolderParameters.IdOptionParametersOrder());

        stage.add(new InputStep("folderBestParams"));
        stage.add(new Step(WriteParams.class, parameters));
        return stage;
    }

    @Verified
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    public static class SplitFoldersForTuneEval implements ExNihiloSource<TfFolderParameters> {

        public Processor<TfFolderParameters> processor;
        long numFolders;
        List<ModelParameters> paramsList;

        public SplitFoldersForTuneEval(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            numFolders = parameters.getJSON().getLong("cvFolderNum");
            paramsList = new GmiParameterSettings(p).getFacetingSettings();
        }

        @Override
        public void run() throws IOException {
            for (int i = 1; i <= numFolders; i++) {
                String folderId = String.valueOf(i);
                for (ModelParameters params : paramsList) {
                    processor.process(new TfFolderParameters(folderId, "tune", params.toString()));
                }
            }
            processor.close();
        }

        public void close() throws IOException {
            processor.close();
        }

        @Override
        public void setProcessor(org.lemurproject.galago.tupleflow.Step nextStage) throws IncompatibleProcessorException {
            Linkage.link(this, nextStage);
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    public static class SplitFoldMetricsAddRanker extends StandardStep<TfFolderParameters, TfFolderParameters> {

        List<String> rankers;

        public SplitFoldMetricsAddRanker(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            rankers = new GmiParameterSettings(p).rankers;
        }

        @Override
        public void process(TfFolderParameters fold) throws IOException {
            for (String ranker : rankers) {
                processor.process(new TfFolderParameters(fold.id, fold.option, ranker));
            }
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    public static class SelectBestParam extends StandardStep<TfFolderParameters, TfFolderParameters> {

        List<ModelParameters> paramsList;
        int facetTuneRank;
        String gmiTuneDir;

        public SelectBestParam(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            paramsList = new GmiParameterSettings(p).getClusteringSettings();
            facetTuneRank = new Long(p.getLong("facetTuneRank")).intValue();
            gmiTuneDir = Utility.getFileName(p.getString("facetTuneDir"), "gmi", "tune");
        }

        @Override
        public void process(TfFolderParameters foldMetric) throws IOException {
            String folderId = foldMetric.id;
            int metricIndex = Integer.parseInt(foldMetric.option);
            String ranker = foldMetric.parameters;

            String evalDir = Utility.getFileName(gmiTuneDir, folderId, "eval");

            double maxScore = Double.NEGATIVE_INFINITY;
            ModelParameters maxParams = null;
            for (ModelParameters params : paramsList) {
                GmiClusterParameters clusterParams = (GmiClusterParameters) params;
                GmiFacetParameters facetParams = new GmiFacetParameters(clusterParams.termProbTh, clusterParams.pairProbTh, ranker);
                File evalFile = new File(Utility.getFacetEvalFileName(evalDir, "gmi", facetParams.toFilenameString(), facetTuneRank));
                double score = QueryMetrics.getAvgScore(evalFile, metricIndex);
                if (score > maxScore) {
                    maxScore = score;
                    maxParams = facetParams;
                }
            }

            foldMetric.parameters = maxParams.toString();
            processor.process(foldMetric);

        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class SplitFoldQueries extends StandardStep<TfFolderParameters, TfQueryParameters> {

        String gmTrainDir;

        public SplitFoldQueries(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            gmTrainDir = Utility.getFileName(p.getString("facetRunDir"), "gm", "train");
        }

        @Override
        public void process(TfFolderParameters folderParams) throws IOException {
            String folderId = folderParams.id;
            int metricIndex = Integer.parseInt(folderParams.option);
            GmiFacetParameters maxParams = new GmiFacetParameters(folderParams.parameters);
            String ranker = maxParams.ranker;

            String testQueryFileName = Utility.getFileName(gmTrainDir, folderId, "test.query");
            TfQuery[] queries = QueryFileParser.loadQueryList(testQueryFileName);
            for (TfQuery q : queries) {
                String folderOptionRankerMetricIndex = Utility.parametersToString(folderId, "predict", ranker, metricIndex);
                processor.process(new TfQueryParameters(q.id, folderOptionRankerMetricIndex,
                        new GmiClusterParameters(maxParams.termProbTh, maxParams.pairProbTh).toString()));
            }
        }
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
    public static class WriteParams implements Processor<TfFolderParameters> {

        String model = "gmi";
        File bestParamFile;
        ArrayList<Selection> selections;

        public WriteParams(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            bestParamFile = new File(Utility.getFileName(p.getString("facetTuneDir"), model, "params"));
            selections = new ArrayList<>();
        }

        @Override
        public void process(TfFolderParameters foldParams) throws IOException {
            String folderId = foldParams.id;
            String metricIndex = foldParams.option;
            GmiFacetParameters facetParams = new GmiFacetParameters(foldParams.parameters);
            GmiClusterParameters clusterParams = new GmiClusterParameters(facetParams.termProbTh, facetParams.pairProbTh);
            selections.add(new Selection(folderId, metricIndex, facetParams.ranker, TextProcessing.join(clusterParams.paramArray, "\t")));
        }

        @Override
        public void close() throws IOException {
            Collections.sort(selections);
            BufferedWriter writer = Utility.getWriter(bestParamFile);
            for (Selection selection : selections) {
                writer.write(String.format("gmi\t%s\t%d\t%d\t%s\n", selection.ranker, selection.metricIndex,
                        selection.folderId, selection.params));
            }
            writer.close();
            Utility.infoWritten(bestParamFile.getAbsoluteFile());
        }

        public static class Selection implements Comparable<Selection> {

            int folderId;
            int metricIndex;
            String ranker;
            String params;

            public Selection(String folderId, String metrixIndex, String ranker, String params) {
                this.folderId = Integer.parseInt(folderId);
                this.metricIndex = Integer.parseInt(metrixIndex);
                this.ranker = ranker;
                this.params = params;
            }

            @Override
            public int compareTo(Selection that) {
                int cmpRanker = this.ranker.compareTo(that.ranker);
                if (cmpRanker == 0) {
                    int metricCmp = this.metricIndex - that.metricIndex;
                    return metricCmp == 0 ? this.folderId - that.folderId : metricCmp;
                } else {
                    return cmpRanker;
                }
            }

        }
    }
}
