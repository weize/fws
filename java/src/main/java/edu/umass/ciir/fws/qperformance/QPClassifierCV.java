/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.qperformance;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.clustering.gm.lr.LinearRegressionModel;
import edu.umass.ciir.fws.eval.CombinedFacetEvaluator;
import static edu.umass.ciir.fws.eval.PrfNewEvaluator.safelyNormalize;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * query performance classifier, cross validation.
 *
 * @author wkong
 */
public class QPClassifierCV {

    final static double[] betas = new double[]{1, 0.5, 0.2, 0.1}; // weight for racall
    String querySplitDir;
    List<Double> thresholds; // for producing labels
    QPfeatureExtractor featureExtractor;
    File queryFile;
    int folderNum;
    int topK;

    HashMap<String, QueryPerformanceFeatures> qpfeatureMap;
    String folderSplitDir;
    String trainDir;
    String predictDir;
    double threshold;
    LinearRegressionModel lrModel;
    OLSMultipleLinearRegressionModel rgModel;
    boolean regression; // regression or classifciation

    public QPClassifierCV(Parameters p) throws IOException {
        querySplitDir = p.getString("querySplitDir");
        featureExtractor = new QPfeatureExtractor();
        thresholds = p.getAsList("qpClassifyThresholds");
        queryFile = new File(p.getString("queryFile"));
        folderNum = (int) p.getLong("cvFolderNum");
        topK = (int) p.getLong("topFacetNum");
        regression = p.getBoolean("qpRegression");
        lrModel = new LinearRegressionModel(p.getAsList("qpFeatureIndices"));
        rgModel = new OLSMultipleLinearRegressionModel(p.getAsList("qpFeatureIndices"));
    }

    public void run(String facetTuneDir, String facetModel, String modelParams,
            String gmPredictDir,
            int metricIdx, String runDir) throws IOException {

        prepareRunDir(runDir, folderNum);

        // extract features
        qpfeatureMap = featureExtractor.run(queryFile, facetTuneDir, facetModel, modelParams, topK,
                gmPredictDir, metricIdx);

        // for each threshold
        for (double t : thresholds) {
            threshold = t;
            String qpClassifyDir = Utility.getFileName(runDir, Utility.parametersToFileNameString(threshold));
            String qpRegressionDir = Utility.getFileName(runDir, "rg_" + Utility.parametersToFileNameString(threshold));
            String dir = regression ? qpRegressionDir : qpClassifyDir;
            prepareQpClassifyDir(dir, folderNum);
            // training and tuning
            for (int i = 1; i <= folderNum; i++) {
                trainTunePredict(String.valueOf(i));
            }
            // eval results
            eval();
        }

    }

    private void trainTunePredict(String folderId) throws IOException {
        String folderDir = Utility.getFileName(trainDir, folderId);
        File trainQueryFile = new File(Utility.getFileName(folderSplitDir, folderId, "train.query")); // input
        File testQueryFile = new File(Utility.getFileName(folderSplitDir, folderId, "test.query")); // input
        File trainFile = new File(Utility.getFileName(folderDir, "train.qp.data.gz")); // output
        File testFile = new File(Utility.getFileName(folderDir, "test.qp.data.gz")); // output

        File modelFile = new File(Utility.getFileName(folderDir, "train.qp.model"));
        File scalerFile = new File(Utility.getFileName(folderDir, "train.qp.scaler"));

        File predictTrainFile = new File(Utility.getFileName(folderDir, "train.qp.predcit")); // for tune
        File predictTrainEvalFile = new File(Utility.getFileName(folderDir, "train.qp.predcit.eval")); // for tune 
        File predictTestFile = new File(Utility.getFileName(folderDir, "test.qp.predcit"));

        prepareDataFile(trainQueryFile, trainFile);
        train(trainFile, modelFile, scalerFile);
        tune(trainFile, modelFile, scalerFile, predictTrainFile, predictTrainEvalFile);

        prepareDataFile(testQueryFile, testFile);
        predict(testFile, modelFile, scalerFile, predictTestFile);

    }

    private void prepareRunDir(String runDir, int folderNum) throws IOException {
        Utility.createDirectory(runDir);

        // load queries of each folder
        ArrayList<TfQuery[]> queryFolders = new ArrayList<>();
        for (int i = 0; i < folderNum; i++) {
            String filename = Utility.getFileName(querySplitDir, "query.0" + i);
            TfQuery[] queries = QueryFileParser.loadQueryList(filename);
            queryFolders.add(queries);
        }

        folderSplitDir = Utility.getFileName(runDir, "fold-splits");
        Utility.createDirectory(folderSplitDir);
        for (int i = 0; i < folderNum; i++) {
            String folderDir = Utility.getFileName(folderSplitDir, String.valueOf(i + 1));
            Utility.createDirectory(folderDir);
            // train.qp.data
            // train.qp.model
            // train.qp.scaler

            // test query
            String testQuery = Utility.getFileName(folderDir, "test.query");
            QueryFileParser.output(queryFolders.get(i), testQuery);
            // train query
            String trainQuery = Utility.getFileName(folderDir, "train.query");
            ArrayList<TfQuery> trainQueries = new ArrayList<>();
            for (int j = 0; j < folderNum; j++) {
                if (j != i) {
                    trainQueries.addAll(Arrays.asList(queryFolders.get(j)));
                }
            }
            QueryFileParser.output(trainQueries.toArray(new TfQuery[0]), trainQuery);
        }
    }

    private void prepareQpClassifyDir(String qpClassifyDir, int folderNum) {
        Utility.createDirectory(qpClassifyDir);

        predictDir = Utility.getFileName(qpClassifyDir, "predict");
        Utility.createDirectory(predictDir);

        trainDir = Utility.getFileName(qpClassifyDir, "train");
        Utility.createDirectory(trainDir);

        for (int i = 0; i < folderNum; i++) {
            String folderDir = Utility.getFileName(trainDir, String.valueOf(i + 1));
            Utility.createDirectory(folderDir);
        }

    }

    private void prepareDataFile(File queryFile, File dataFile) throws IOException {
        List<TfQuery> queries = QueryFileParser.loadQueries(queryFile);
        // create train feature file
        BufferedWriter writer = Utility.getWriter(dataFile);

        for (TfQuery query : queries) {
            QueryPerformanceFeatures qpf = qpfeatureMap.get(query.id);

            int label = qpf.score >= threshold ? 1 : -1;
            writer.write(String.format("%d\t%s\t#%d\t%s\t%s\t%f\n", label, qpf.featuresToString(), label, query.id, query.text, qpf.score));

        }
        writer.close();
        Utility.infoWritten(dataFile);
    }

    private void train(File trainFile, File modelFile, File scalerFile) throws IOException {
        if (regression) {
            try {
                rgModel.train(trainFile, modelFile, scalerFile);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            lrModel.train(trainFile, modelFile, scalerFile);
        }
        Utility.infoWritten(modelFile);
        Utility.infoWritten(scalerFile);
    }

    private void predict(File testFile, File modelFile, File scalerFile, File predictTestFile) throws IOException {
        if (regression) {
            rgModel.predict(testFile, modelFile, scalerFile, predictTestFile);
        } else {
            lrModel.predict(testFile, modelFile, scalerFile, predictTestFile);
        }
        Utility.infoWritten(predictTestFile);
    }

    private void tune(File trainFile, File modelFile, File scalerFile, File predictTrainFile, File predictTrainEvalFile) throws IOException {
        if (regression) {
            rgModel.predict(trainFile, modelFile, scalerFile, predictTrainFile);
        } else {
            lrModel.predict(trainFile, modelFile, scalerFile, predictTrainFile);
        }
        Utility.infoWritten(predictTrainFile);

        // tuen F1
        BufferedReader reader = Utility.getReader(predictTrainFile);
        String line;
        ArrayList<Prediction> results = new ArrayList<>(); // hack: using item as label, and score as prob
        int posTotal = 0;
        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            double prob = Double.parseDouble(elems[0]);
            int label = Integer.parseInt(elems[1]);
            Prediction p = new Prediction(prob, label);
            if (label > 0) {
                posTotal++;
            }
            results.add(p);
        }

        Collections.sort(results);
        int posReturn = 0;
        int sizeReturn = 0;

        int size = betas.length;
        double[] fBest = new double[size]; // defualt is 0;
        double[] pBest = new double[size]; // precision
        double[] rBest = new double[size]; // recall
        double[] tBest = new double[size]; // thresholds
        Arrays.fill(fBest, 0.0);
        Arrays.fill(tBest, 1.0);
        for (Prediction result : results) {
            sizeReturn++;
            if (result.label > 0) {
                posReturn++;
            }
            double precision = (double) posReturn / (double) sizeReturn;
            double recall = safelyNormalize(posReturn, posTotal);
            for (int i = 0; i < size; i++) {
                double f = CombinedFacetEvaluator.Fmeasure(precision, recall, betas[i]);
                if (f > fBest[i]) {
                    fBest[i] = f;
                    pBest[i] = precision;
                    rBest[i] = recall;
                    tBest[i] = result.prob;
                }
            }
        }

        BufferedWriter writer = Utility.getWriter(predictTrainEvalFile);
        for (int i = 0; i < size; i++) {
            writer.write(String.format("%.16g\t%.4f\t%.4f\t%.4f\n", tBest[i], fBest[i], pBest[i], rBest[i]));
        }

        writer.close();
        Utility.infoWritten(predictTrainEvalFile);
    }

    private void eval() throws IOException {
        File predictFile = new File(Utility.getFileName(predictDir, "test.qp.predcit"));
        File evalFile = new File(Utility.getFileName(predictDir, "test.qp.predcit.eval"));

        int size = betas.length; // size of tuen metrics F1, F0.5 ....
        int atotal = 0; // annotator total
        int[] stotal = new int[size + 1]; // size total return
        int[] correct = new int[size + 1]; // correct ones: true positive
        int[] ncorrect = new int[size + 1]; // true negative
        double[] avgPerfs = new double[size + 1];
        int[] predictLabels = new int[size + 1];

        int total = 0;
        ArrayList<Prediction> predictions = new ArrayList<>();
        double rmsd = 0;
        ArrayList<Double> perfs = new ArrayList<>();
        ArrayList<Double> probs = new ArrayList<>();
        for (int i = 1; i <= folderNum; i++) {
            String folderId = String.valueOf(i);
            String folderDir = Utility.getFileName(trainDir, folderId);
            File trainEvalFile = new File(Utility.getFileName(folderDir, "train.qp.predcit.eval")); // for tune
            File testPredictFile = new File(Utility.getFileName(folderDir, "test.qp.predcit"));

            // get threshold tuned based on different metrics
            // add an extra one to get results of predicting everyting as postive
            double[] tunedThresholds = readThresholds(trainEvalFile);

            BufferedReader reader = Utility.getReader(testPredictFile);
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                String[] elems = line.split("\t");
                double perf = Double.parseDouble(elems[4]); // performance
                double prob = Double.parseDouble(elems[0]); // prob or regression results
                double diff = prob - perf; // 
                perfs.add(perf);
                probs.add(prob);
                rmsd += diff * diff;
                int label = Integer.parseInt(elems[1]);
                if (label == 1) {
                    atotal++;
                }

                // for each metrics
                for (int j = 0; j <= size; j++) {
                    double th = tunedThresholds[j];
                    int predictLabel = prob >= th ? 1 : -1;
                    predictLabels[j] = predictLabel;

                    if (predictLabel == 1) {
                        stotal[j]++;
                        avgPerfs[j] += perf;
                        if (label == 1) {
                            correct[j]++;
                        }
                    } else {
                        if (label != 1) {
                            ncorrect[j]++; // true positive
                        }
                    }
                }

                predictions.add(new Prediction(prob,
                        String.format("%s\t%s\n", TextProcessing.join(predictLabels, "\t"), line)));
            }
            reader.close();
        }

        // r squared
        double rsquared = rSquared(perfs, probs);
        double correlation = correlation(perfs, probs);

        rmsd = Math.sqrt(rmsd / total);
        int natotal = total - atotal; // # negatives in truth data

        Collections.sort(predictions);

        BufferedWriter writer = Utility.getWriter(predictFile);
        for (Prediction p : predictions) {
            writer.write(p.info);
        }

        writer.close();
        Utility.infoWritten(predictFile);

        BufferedWriter writerEval = Utility.getWriter(evalFile);

        writerEval.write("#return\tAvgPerf\tP\tR\tTNR\tF1\tRMSD\tRSq\tcor\n");
        for (int j = 0; j <= size; j++) {
            double precision = safelyNormalize(correct[j], stotal[j]);
            double recall = safelyNormalize(correct[j], atotal);
            double tureNegativeRate = safelyNormalize(ncorrect[j], natotal);
            double f1 = CombinedFacetEvaluator.f1(precision, recall);
            avgPerfs[j] = safelyNormalize(avgPerfs[j], stotal[j]);

            writerEval.write(String.format("%d\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\n",
                    stotal[j], avgPerfs[j], precision, recall, tureNegativeRate, f1, rmsd, rsquared, correlation));
        }

        writerEval.close();

        Utility.infoWritten(evalFile);

    }

    private double[] readThresholds(File trainEvalFile) throws IOException {
        BufferedReader reader = Utility.getReader(trainEvalFile);
        String line;
        double[] ths = new double[betas.length + 1];
        ths[0] = -0.1;
        int i = 1;
        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            ths[i++] = Double.parseDouble(elems[0]);
        }
        reader.close();
        return ths;
    }

    private double rSquared(ArrayList<Double> perfs, ArrayList<Double> probs) {
        double perfAvg = 0;
        for (double one : perfs) {
            perfAvg += one;
        }
        perfAvg /= perfs.size();

        double sTotal = 0;
        for (double one : perfs) {
            double diff = one - perfAvg;
            sTotal += diff * diff;
        }

        double sRes = 0;
        for (int i = 0; i < perfs.size(); i++) {
            double diff = perfs.get(i) - probs.get(i);
            sRes += diff * diff;
        }

        return 1 - sRes / sTotal;
    }

    private double correlation(ArrayList<Double> perfs, ArrayList<Double> probs) {
        PearsonsCorrelation PearsonsCorrelation = new PearsonsCorrelation();
        return PearsonsCorrelation.correlation(unbox(perfs), unbox(probs));
    }

    public double[] unbox(List<Double> perfs) {
        double[] ret = new double[perfs.size()];
        for (int i = 0; i < perfs.size(); i++) {
            ret[i] = perfs.get(i);
        }
        return ret;
    }

    public static class Prediction implements Comparable<Prediction> {

        double prob;
        int label; // ground truth
        String info;

        public Prediction(double prob, int label) {
            this.prob = prob;
            this.label = label;
        }

        public Prediction(double prob, String info) {
            this.prob = prob;
            this.info = info;
        }

        @Override
        public int compareTo(Prediction o) {
            return Double.compare(o.prob, this.prob);
        }

    }

}
