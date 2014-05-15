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
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public LinearRegressionModel() {
        selectedFeatureIndices = null;
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
                x[i] = new FeatureNode(i + 1, value);
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
}
