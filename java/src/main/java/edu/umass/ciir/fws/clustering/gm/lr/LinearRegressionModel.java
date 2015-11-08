/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.lr;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import edu.umass.ciir.fws.feature.Features;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

/**
 *
 * @author wkong
 */
public class LinearRegressionModel {

    /**
     * Indices of selected features in the feature file. Format: label feature1
     * feature2 ... feature_n # comment.
     *
     * e.g. feature1 is at index 1
     */
    int[] selectedFeatureIndices;

    public Problem prob;
    StandardScaler scaler;
    String[] comments; // comments for each data point
    final static double bias = 1.0;

    // demo add
    Model model;

    public LinearRegressionModel() {
        selectedFeatureIndices = null;
    }

    public LinearRegressionModel(List<Long> selectedFeatureIndices) throws IOException {
        // ensure the indices are ordered and distinct.
        long last = 0;
        for (long cur : selectedFeatureIndices) {
            if (cur <= last) {
                throw new IOException("Bad feature selection");
            }
            last = cur;
        }

        this.selectedFeatureIndices = new int[selectedFeatureIndices.size()];
        for (int i = 0; i < selectedFeatureIndices.size(); i++) {
            this.selectedFeatureIndices[i] = selectedFeatureIndices.get(i).intValue();
        }
    }

    public LinearRegressionModel(int[] selectedFeatureIndices) throws IOException {
        // ensure the indices are ordered and distinct.
        int last = 0;
        for (int cur : selectedFeatureIndices) {
            if (cur <= last) {
                throw new IOException("Bad feature selection");
            }
            last = cur;
        }
        this.selectedFeatureIndices = Arrays.copyOf(selectedFeatureIndices, selectedFeatureIndices.length);
    }

    public void train(File featureFile, File modelFile, File scalerFile) throws IOException {
        readProblem(featureFile);

        // scale features
        scaler = new StandardScaler();
        scaler.fitTransform(prob);
        scaler.save(scalerFile); // save scaler to file

        // trainning
        SolverType solver = SolverType.L2R_LR; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.0001; // stopping criteria
        Parameter parameter = new Parameter(solver, C, eps);
        Model model = Linear.train(prob, parameter);
        model.save(modelFile);
    }

    public void load(File modelFile, File scalerFile, int[] featureIndices) throws IOException {
        scaler = new StandardScaler(scalerFile); // load from file
        selectedFeatureIndices = featureIndices;
        model = Linear.loadModel(modelFile);
    }

    public double predict(Features features) {
        // features
        Feature[] x = convertFeatures(features);
        scaler.transform(x);
        Feature[] fs = x;
        double[] prob_estimates = new double[2];
        Linear.predictProbability(model, x, prob_estimates);
        return prob_estimates[0];
    }

    public void predict(File featureFile, File modelFile, File scalerFile, File preditFile) throws IOException {
        readProblem(featureFile);
        scaler = new StandardScaler(scalerFile); // load from file
        scaler.transform(prob);

        BufferedWriter writer = Utility.getWriter(preditFile);
        Model model = Linear.loadModel(modelFile);
        double[] prob_estimates = new double[2];
        assert model.getLabels()[0] == 1.0 : "defulat class label not 1";
        for (int i = 0; i < prob.l; i++) {
            Feature[] fs = prob.x[i];
            Linear.predictProbability(model, fs, prob_estimates);
            writer.write(String.format("%f\t%s\n", prob_estimates[0], comments[i]));
        }
        writer.close();
    }

    public void readProblem(File file) throws IOException {
        prob = new Problem();
        prob.bias = bias;

        BufferedReader reader = Utility.getReader(file);
        List<Double> vy = new ArrayList<>();
        List<Feature[]> vx = new ArrayList<>();
        ArrayList<String> cmts = new ArrayList<>();

        // label feature1 feature2 ... feature_n # comment
        String line = reader.readLine();
        int filedNum = line.split("#")[0].trim().split("\t").length;

        // selected all features if not specified
        if (selectedFeatureIndices == null) {
            selectedFeatureIndices = new int[filedNum - 1];
            for (int i = 0; i < selectedFeatureIndices.length; i++) {
                selectedFeatureIndices[i] = i + 1;
            }
        }

        prob.n = selectedFeatureIndices.length + 1; // need to inclue bias

        do {
            String[] dataComment = line.split("#", 2);
            String dataStr = dataComment[0].trim();
            String comment = dataComment.length == 2 ? dataComment[1].trim() : "";

            //add comments
            cmts.add(comment);

            //add data
            String[] fields = dataStr.split("\t");
            assert fields.length == filedNum : "Number of fields inconsistent: " + line;

            // label
            double label = Double.parseDouble(fields[0]);
            vy.add(label);

            // features
            Feature[] x = new Feature[prob.n]; // includes bias
            for (int i = 0; i < selectedFeatureIndices.length; i++) {
                Double value = Double.parseDouble(fields[selectedFeatureIndices[i]]);
                x[i] = new FeatureNode(i + 1, value); // ?? feature index start from 1?
            }
            x[prob.n - 1] = new FeatureNode(prob.n, bias);
            vx.add(x);

        } while ((line = reader.readLine()) != null);

        comments = cmts.toArray(new String[0]);

        prob.l = vy.size();
        prob.x = vx.toArray(new Feature[0][]);
        prob.y = new double[prob.l];
        for (int i = 0; i < prob.l; i++) {
            prob.y[i] = vy.get(i);
        }

    }

    public void readProblem(TreeMap<String, Features> features) {
        prob = new Problem();
        prob.bias = bias;

        // selected all features if not specified
        if (selectedFeatureIndices == null) {
            selectedFeatureIndices = new int[1 - 1];
            for (int i = 0; i < selectedFeatureIndices.length; i++) {
                selectedFeatureIndices[i] = i + 1;
            }
        }

    }

    public void printProblem(PrintStream output) {
        output.println("l: " + prob.l);
        output.println("n: " + prob.n);
        output.println("y\t###\tcomment\n");
        for (int i = 0; i < prob.y.length; i++) {
            output.println(prob.y[i] + "\t###\t" + comments[i]);
        }
        for (Feature[] fs : prob.x) {
            for (Feature f : fs) {
                output.print(f.getIndex() + ":" + f.getValue() + " ");
            }
            output.println();
        }
    }

    public void scaleFeatures(File scalerFile) throws IOException {
        scaler = new StandardScaler();
        scaler.fitTransform(prob);
        scaler.save(scalerFile);
    }

    private Feature[] convertFeatures(Features features) {
        int size = selectedFeatureIndices.length;
        Feature[] x = new Feature[size + 1]; // includes bias
        for (int i = 0; i < selectedFeatureIndices.length; i++) {
            // feature index in Class features start from 0, but in LableFeature file start from 1
            Object val = features.getFeature(selectedFeatureIndices[i] - 1);
            double doubleVal;            
            if (val instanceof Integer) {
                doubleVal = ((Integer) val).doubleValue();
            } else if (val instanceof Double) {
                doubleVal = (Double) val;
            } else {
                throw new RuntimeException(val.getClass()+ " : " + val + ",  is not supported as a feature");
            }
            x[i] = new FeatureNode(i + 1, doubleVal);
        }
        x[size] = new FeatureNode(size + 1, bias);
        return x;
    }

}
