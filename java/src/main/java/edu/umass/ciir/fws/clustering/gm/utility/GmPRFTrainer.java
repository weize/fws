/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.utility;

import cc.mallet.optimize.GradientAscent;
import cc.mallet.optimize.LimitedMemoryBFGS;
import edu.emory.mathcs.backport.java.util.Arrays;
import static edu.umass.ciir.fws.clustering.gm.utility.StandardScaler.minStd;
import edu.umass.ciir.fws.utility.TextProcessing;
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
    double c = 0;
    static Random random = new Random();
    List<Integer> tfIndices;
    List<Integer> pfIndices;
    double[] paramStart;
    String optimizerName;

    public GmPRFTrainer(Parameters p) {
        this(p.getAsList("termFeatureIndices"), p.getAsList("pairFeatureIndices"));
        tolerance = p.get("gmTrainTolerance", 0.0001);
        maxIterations = (int) p.get("gmTrainIteration", 1500);
        nStart = (int) p.get("gmTrainRestart", 10);
        alpha = p.get("gmPRFAlpha", 1.0);
        beta = p.get("gmPRFBeta", 1.0);
        c = p.getDouble("gmRegularizer");
        optimizerName = p.getString("gmOptimizer");
        List<Double> paramInitialization = p.getAsList("gmParamInitial");
        paramStart = new double[paramInitialization.size()];
        for (int i = 0; i < paramStart.length; i++) {
            paramStart[i] = paramInitialization.get(i);
        }

        Utility.info(String.format("optimizer=%s", optimizerName));
        Utility.info(String.format("alpha=%f, beta=%f, c=%f", alpha, beta, c));
        Utility.info(String.format("tolerance=%f, maxIterations=%d, nStart=%d", tolerance, maxIterations, nStart));
        Utility.info(String.format("termFeatures=[%s]", TextProcessing.join(tfIndices, ", ")));
        Utility.info(String.format("pairFeatures=[%s]", TextProcessing.join(pfIndices, ", ")));
        Utility.info(String.format("intialize=[%s]", TextProcessing.join(paramStart, ", ")));
//        double[] params = new double[]{
//            -0.3762050277171397, -0.1767789384657117, 0.2636189780605572, 0.01443498082458111, 0.07693741865527474, -0.2746843538121086, 0.1784435302779528, -0.2777087551505914, -0.1574900052990597, 0.6340636025733660, -0.3905306459439568, -0.1597969204354845, 0.5026913743301520, 0.08568241667767658, 0.1097080410269183, -0.1855926707185300, -0.03751924070834197, -0.1912785856713757, 0.4260920554060768, 0.06864309267355007, -0.02752296230909643, -0.05769599025371386, 0.04740505796407326, 0.5248502726122284, -0.4313215258514472, 0.6328677061102274, -0.07293937195128440, -0.5784744536227051, 0.1416276249252304, 0.8923167305734128, -7.649110438647697,
//            0.01834205965655030, 0.08698109808524315, 1.175087396030538, 0.7652535509044945, -1.951643915232145
//        };        
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

        GmPRFMaximizable optimizable = new GmPRFMaximizable(instances, alpha, beta, c);

        double bestScore = Double.NEGATIVE_INFINITY;
        double bestScorePRF = Double.NEGATIVE_INFINITY;
        double[] bestParams = new double[optimizable.nf];

        for (int i = 0; i < nStart; i++) {
            if (i == 0) {
                optimizable.setParameters(paramStart);
            } else {
                initializeParams(optimizable);
            }

            if (optimizerName.equals("gradientAscent")) {
                GradientAscent optimizer = new GradientAscent(optimizable);
                optimizer.setTolerance(tolerance);
                optimizer.optimize(maxIterations);
            } else {
                LimitedMemoryBFGS optimizer = new LimitedMemoryBFGS(optimizable);
                optimizer.setTolerance(tolerance);
                optimizer.optimize(maxIterations);
            }

            double curScore = optimizable.getValue();
            if (curScore > bestScore) {
                bestScore = curScore;
                bestScorePRF = optimizable.getPRFValue();
                optimizable.getParameters(bestParams);
            }
            Utility.info("random start " + (i + 1) + "\tPRFrl=" + bestScore + "\tPRF=" + bestScorePRF);
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
            writer.write(String.format("%.16g \n", param));
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
