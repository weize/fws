/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.utility;

import cc.mallet.optimize.LimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;
import cc.mallet.optimize.Optimizer;
import edu.emory.mathcs.backport.java.util.Arrays;
import static edu.umass.ciir.fws.clustering.gm.utility.StandardScaler.minStd;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author wkong
 */
public class GmuModel implements Optimizable.ByGradientValue {

    static Random random = new Random();

    int nStart = 10;
    Instance[] instances;
    int nTf, nPf; // number of term features, and number of pair features, including bias
    int nf; // nTf + nPf
    List<Integer> tfIndices;
    List<Integer> pfIndices;
    double[] tParams; // term params: w_i
    double[] pParams; // pair params: u_i

    public GmuModel(List<Long> tfIndices, List<Long> pfIndices) {
        this.tfIndices = new ArrayList<>(tfIndices.size());
        for (long tfIdx : tfIndices) {
            this.tfIndices.add((int) tfIdx);
        }
        this.pfIndices = new ArrayList<>(pfIndices.size());

        for (long pfIdx : pfIndices) {
            this.pfIndices.add((int) pfIdx);
        }
    }

    public void train(File tFeatureFile, File pFeatureFile, File tModelFile, File pModelFile, File tScalerFile, File pScalerFile) throws IOException {
        instances = Instance.readInstances(tFeatureFile, pFeatureFile, tfIndices, pfIndices);
        nTf = instances[0].tFeatures[0].length;
        nPf = instances[0].pFeatures[0].length;
        nf = nTf + nPf;
        tParams = new double[nTf];
        pParams = new double[nPf];

        // normalize data
        normalizeTermFeatures(tScalerFile);
        normalizePairFeatures(pScalerFile);

        double bestScore = Double.NEGATIVE_INFINITY;
        double[] bestTParams = null;
        double[] bestPParams = null;

        for (int i = 0; i < nStart; i++) {
            initializeParams();
            Optimizer bfgs = new LimitedMemoryBFGS(this);
            bfgs.optimize();
            double curScore = this.getValue();
            if (curScore > bestScore) {
                bestScore = curScore;
                bestTParams = Arrays.copyOf(tParams, nTf);
                bestPParams = Arrays.copyOf(pParams, nPf);
            }
        }

        saveModel(bestTParams, tModelFile);
        saveModel(bestPParams, pModelFile);

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
            writer.write(String.format("%.16g", param));
        }
        writer.close();
    }

    private void initializeParams() {
        double rangeMin = -2;
        double rangeMax = +2;

        for (int i = 0; i < nTf; i++) {
            tParams[i] = random(rangeMin, rangeMax);
        }

        for (int i = 0; i < nPf; i++) {
            pParams[i] = random(rangeMin, rangeMax);
        }

    }

    public double random(double rangeMin, double rangeMax) {
        return rangeMin + (rangeMax - rangeMin) * random.nextDouble();
    }

    private void normalizeTermFeatures(File scalerFile) throws IOException {
        // fit data: find means and stds
        int n = nTf;
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

    private void normalizePairFeatures(File scalerFile) throws IOException {

        int n = nPf;
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

    public final void saveScaler(double[] means, double[] stds, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        writer.write("#mean\tstd\n");
        for (int i = 0; i < means.length; i++) {
            writer.write(String.format("%f\t%f\n", means[i], stds[i]));
        }
        writer.close();
    }

    @Override
    public void getValueGradient(double[] buffer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getValue() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNumParameters() {
        return this.nf;
    }

    @Override
    public void getParameters(double[] buffer) {
        int i = 0;
        for (int k = 0; k < nTf; k++) {
            buffer[i++] = tParams[k];
        }

        for (int k = 0; k < nPf; k++) {
            buffer[i++] = pParams[k];
        }
    }

    @Override
    public double getParameter(int index) {
        return index < nTf ? tParams[index] : pParams[index - nTf];
    }

    @Override
    public void setParameters(double[] params) {
        int i = 0;
        for (int k = 0; k < nTf; k++) {
            tParams[k] = params[i++];
        }

        for (int k = 0; k < nPf; k++) {
            pParams[k] = params[i++];
        }

    }

    @Override
    public void setParameter(int index, double value) {
        if (index < nTf) {
            tParams[index] = value;
        } else {
            pParams[index - nTf] = value;
        }
    }

}
