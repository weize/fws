/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.utility;

import cc.mallet.optimize.GradientAscent;
import edu.emory.mathcs.backport.java.util.Arrays;
import static edu.umass.ciir.fws.clustering.gm.utility.StandardScaler.minStd;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class GmPRFTrainer {

    double tolerance = 0.0001;
    int maxIterations = 1500;
    int nStart = 10;
    double alpha = 1;
    double beta = 1;
    static Random random = new Random();
    List<Integer> tfIndices;
    List<Integer> pfIndices;

    public GmPRFTrainer(Parameters p) {
        tolerance = p.get("gmTrainTolerance", 0.0001);
        maxIterations = (int) p.get("gmTrainIteration", 1500);
        nStart = (int) p.get("gmTrainRestart", 10);
        alpha = p.get("gmPRFAlpha", 1.0);
        beta = p.get("gmPRFBeta", 1.0);
        this.tfIndices = new ArrayList<>(tfIndices.size());
        for (long tfIdx : tfIndices) {
            this.tfIndices.add((int) tfIdx);
        }
        this.pfIndices = new ArrayList<>(pfIndices.size());

        for (long pfIdx : pfIndices) {
            this.pfIndices.add((int) pfIdx);
        }
    }

    public GmPRFTrainer(List<Long> tfIndices, List<Long> pfIndices) {
        this.tfIndices = new ArrayList<>(tfIndices.size());
        for (long tfIdx : tfIndices) {
            this.tfIndices.add((int) tfIdx);
        }
        this.pfIndices = new ArrayList<>(pfIndices.size());

        for (long pfIdx : pfIndices) {
            this.pfIndices.add((int) pfIdx);
        }
    }

    public void train(File tFeatureFile, File pFeatureFile, File tModelFile,
            File pModelFile, File tScalerFile, File pScalerFile) throws IOException {
        Instance[] instances = Instance.readInstances(tFeatureFile, pFeatureFile, tfIndices, pfIndices);
        // normalize data
        normalizeTermFeatures(instances, tScalerFile);
        normalizePairFeatures(instances, pScalerFile);

        GmPRFMaximizable optimizable = new GmPRFMaximizable(instances, alpha, beta);

        double bestScore = Double.NEGATIVE_INFINITY;
        double[] bestParams = new double[optimizable.nf];

        for (int i = 0; i < nStart; i++) {

            initializeParams(optimizable);
            GradientAscent bfgs = new GradientAscent(optimizable);
            //Optimizer bfgs = new LimitedMemoryBFGS(optimizable);
            bfgs.setTolerance(tolerance);
            bfgs.optimize(maxIterations);

            double curScore = optimizable.getValue();
            if (curScore > bestScore) {
                bestScore = curScore;
                optimizable.getParameters(bestParams);
            }
            Utility.info("random start " + (i + 1) + "\tPRF=" + bestScore);
        }

        saveModel(Arrays.copyOfRange(bestParams, 0, optimizable.nTf), tModelFile);
        saveModel(Arrays.copyOfRange(bestParams, optimizable.nTf, optimizable.nf), pModelFile);

    }

    /**
     * same format as Logistic regression model
     *
     * @param params
     * @param modelFile
     * @throws IOException
     */
    private void saveModel(double[] params, File modelFile) throws IOException {
//solver_type L2R_LR
//nr_class 2
//label 1 -1
//nr_feature 4
//bias 1.000000000000000
//w
//0.05444528288982231 
//0.07815176802231279 
//1.505038181410305 
//0.7336130091798243 
//-0.7325239074393020

        BufferedWriter writer = Utility.getWriter(modelFile);
        writer.write("solver_type L2R_LR\n");
        writer.write("nr_class 2\n");
        writer.write("label 1 -1\n");
        writer.write(String.format("nr_feature %d\n", params.length - 1));
        writer.write(String.format("bias %.16g\n", 1.0));
        writer.write("w\n");
        for (double param : params) {
            writer.write(String.format("%.16g\n", param));
        }
        writer.close();
    }

    private void initializeParams(GmPRFMaximizable optimizable) {
        double rangeMin = -2;
        double rangeMax = +2;

        for (int i = 0; i < optimizable.nf; i++) {
            optimizable.setParameter(i, random(rangeMin, rangeMax));
        }
    }

    public double random(double rangeMin, double rangeMax) {
        return rangeMin + (rangeMax - rangeMin) * random.nextDouble();
    }

    public static void normalizeTermFeatures(Instance[] instances, File scalerFile) throws IOException {
        // fit data: find means and stds
        int n = instances[0].tFeatures[0].length;
        double[] means = new double[n - 1]; // exclude bias
        double[] stds = new double[n - 1]; // exclude bias

        int count = 0;
        for (Instance instance : instances) {
            for (double[] features : instance.tFeatures) { // all terms 
                for (int k = 0; k < n - 1; k++) {
                    means[k] += features[k];
                }
            }
            count += instance.tFeatures.length;
        }

        Utility.avg(means, count);

        for (Instance instance : instances) {
            for (double[] features : instance.tFeatures) { // all terms 
                for (int k = 0; k < n - 1; k++) {
                    double diff = features[k] - means[k];
                    stds[k] += diff * diff;
                }
            }
        }

        for (int i = 0; i < stds.length; i++) {
            stds[i] = Math.sqrt(stds[i] / (count - 1));
        }

        saveScaler(means, stds, scalerFile);

        // transform data
        for (int k = 0; k < n - 1; k++) {
            if (stds[k] > minStd) {
                for (Instance instance : instances) {
                    for (double[] features : instance.tFeatures) { // all terms 
                        features[k] = (features[k] - means[k]) / stds[k];
                    }
                }
            }
        }

    }

    public static void normalizePairFeatures(Instance[] instances, File scalerFile) throws IOException {

        int n = instances[0].pFeatures[0].length;;
        double[] means = new double[n - 1]; // exclude bias
        double[] stds = new double[n - 1]; // exclude bias

        int count = 0;
        for (Instance instance : instances) {
            for (double[] features : instance.pFeatures) { // all terms 
                for (int k = 0; k < n - 1; k++) {
                    means[k] += features[k];
                }
            }
            count += instance.pFeatures.length;
        }

        Utility.avg(means, count);

        for (Instance instance : instances) {
            for (double[] features : instance.pFeatures) { // all terms 
                for (int k = 0; k < n - 1; k++) {
                    double diff = features[k] - means[k];
                    stds[k] += diff * diff;
                }
            }
        }

        for (int i = 0; i < stds.length; i++) {
            stds[i] = Math.sqrt(stds[i] / (count - 1));
        }

        saveScaler(means, stds, scalerFile);

        // transform data
        for (int k = 0; k < n - 1; k++) {
            if (stds[k] > minStd) {
                for (Instance instance : instances) {
                    for (double[] features : instance.pFeatures) { // all terms 
                        features[k] = (features[k] - means[k]) / stds[k];
                    }
                }
            }
        }

    }

    public static final void saveScaler(double[] means, double[] stds, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        writer.write("#mean\tstd\n");
        for (int i = 0; i < means.length; i++) {
            writer.write(String.format("%f\t%f\n", means[i], stds[i]));
        }
        writer.close();
    }

}
