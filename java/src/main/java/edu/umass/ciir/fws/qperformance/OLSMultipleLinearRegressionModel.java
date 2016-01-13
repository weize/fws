/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.qperformance;

import edu.umass.ciir.fws.clustering.gm.lr.StandardScaler;
import edu.umass.ciir.fws.clustering.gm.utility.Instance;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

/**
 *
 * @author wkong
 */
public class OLSMultipleLinearRegressionModel {

    final static double bias = 1.0;

    double[] y;
    double[][] x;
    String[] comments;
    List<Integer> fIndices;

    public OLSMultipleLinearRegressionModel(List<Long> selectedFeatureIndices) throws IOException {
        fIndices = new ArrayList<>(selectedFeatureIndices.size());
        for (Long idx : selectedFeatureIndices) {
            fIndices.add((int) ((long) idx));
        }
    }

    public void train(File featureFile, File modelFile, File scalerFile) throws IOException {

        // read data
        //writer.write(String.format("%d\t%s\t#%d\t%s\t%s\t%f\n", label, qpf.featuresToString(), label, query.id, query.text, qpf.score));
        loadData(featureFile);
        scale(scalerFile);
        normalizeFeatures(scalerFile);
        // train
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.setNoIntercept(true);
        regression.newSampleData(y, x);
        double[] beta = regression.estimateRegressionParameters();
        saveModel(beta, modelFile);

    }

    public void predict(File featureFile, File modelFile, File scalerFile, File predictFile) throws IOException {
        loadData(featureFile);
        normalizeFeatures(scalerFile);

        BufferedWriter writer = Utility.getWriter(predictFile);
        double[] beta = loadMode(modelFile);
        for (int i = 0; i < x.length; i++) {
            double[] features = x[i];
            double predict = dotProduct(features, beta);
            writer.write(String.format("%f\t%s\n", predict, comments[i]));
        }

        writer.close();

    }

    public static double dotProduct(double[] m1, double[] m2) {
        assert (m1.length == m2.length) : "m1.length != m2.length\n";
        double ret = 0.0;
        for (int i = 0; i < m1.length; i++) {
            ret += m1[i] * m2[i];
        }
        return ret;
    }

    public void normalizeFeatures(File scalerFile) throws IOException {
        StandardScaler scaler = new StandardScaler(scalerFile);
        for (double[] features : x) {
            for (int i = 0; i < features.length - 1; i++) {
                if (scaler.stds[i] > StandardScaler.minStd) {
                    features[i] = (features[i] - scaler.means[i]) / scaler.stds[i];
                }
            }
        }

    }

    private void saveModel(double[] beta, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (double b : beta) {
            writer.write(String.format("%.16g\n", b));
        }
        writer.close();
    }

    private void loadData(File featureFile) throws IOException {
        BufferedReader reader = Utility.getReader(featureFile);
        String line;
        ArrayList<String> lines = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        int size = lines.size();
        reader.close();

        y = new double[size];
        x = new double[size][];
        comments = new String[size];

        for (int i = 0; i < lines.size(); i++) {
            line = lines.get(i);
            String[] dataComment = line.split("#", 2);
            String comment = dataComment[1].trim();
            comments[i] = comment;
            double score = Double.parseDouble(comment.split("\t")[3]);
            String dataStr = dataComment[0].trim();
            String[] fields = dataStr.split("\t");

            y[i] = score;
            double[] features = new double[fIndices.size() + 1]; // one additional for bias
            for (int j = 0; j < fIndices.size(); j++) {
                features[j] = Double.parseDouble(fields[fIndices.get(j)]);
            }
            features[features.length - 1] = bias;
            x[i] = features;
        }

    }

    private void scale(File scalerFile) throws IOException {

        int n = x[0].length;
        double[] means = new double[n - 1]; // exclude bias
        double[] stds = new double[n - 1]; // exclude bias

        int count = x.length;

        for (double[] features : x) { // all terms 
            for (int k = 0; k < n - 1; k++) {
                means[k] += features[k];
            }
        }

        Utility.avg(means, count);

        for (double[] features : x) { // all terms 
            for (int k = 0; k < n - 1; k++) {
                double diff = features[k] - means[k];
                stds[k] += diff * diff;
            }
        }

        for (int i = 0; i < stds.length; i++) {
            stds[i] = Math.sqrt(stds[i] / (count - 1));
        }

        saveScaler(means, stds, scalerFile);
    }

    public static final void saveScaler(double[] means, double[] stds, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        writer.write("#mean\tstd\n");
        for (int i = 0; i < means.length; i++) {
            writer.write(String.format("%f\t%f\n", means[i], stds[i]));
        }
        writer.close();
    }

    private double[] loadMode(File modelFile) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        String line;
        BufferedReader reader = Utility.getReader(modelFile);
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();
        double[] beta = new double[lines.size()];
        for (int i = 0; i < beta.length; i++) {
            beta[i] = Double.parseDouble(lines.get(i));
        }
        return beta;
    }
}
