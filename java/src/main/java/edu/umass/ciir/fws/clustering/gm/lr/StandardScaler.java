/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.lr;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Problem;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author wkong
 */
public class StandardScaler {

    /**
     * Do not standardize the feature if the std is very small.
     *
     */
    public static final double minStd = 0.000001;
    public double[] means;
    public double[] stds;
    
    public StandardScaler() {
        
    }
    
    public StandardScaler(File file) throws IOException {
        load(file);
    }

    /**
     * Calculate means and standard deviations.
     *
     * @param prob
     */
    public void fit(Problem prob) {
        means = new double[prob.n - 1]; // do not inclue the bias
        stds = new double[prob.n - 1];
        int[] counts = new int[prob.n - 1];

        // means
        for (Feature[] fs : prob.x) {
            for (Feature f : fs) {
                int idx = f.getIndex() - 1;
                if (idx != prob.n - 1) { // not the bias
                    means[idx] += f.getValue();
                    counts[idx]++;
                }
            }
        }

        for (int i = 0; i < means.length; i++) {
            means[i] /= counts[i];
        }

        // standard deviation
        for (Feature[] fs : prob.x) {
            for (Feature f : fs) {
                int idx = f.getIndex() - 1;
                if (idx != prob.n - 1) { // not the bias
                    double val = f.getValue();
                    stds[idx] += (val - means[idx]) * (val - means[idx]);
                }
            }
        }

        for (int i = 0; i < stds.length; i++) {
            stds[i] = Math.sqrt(stds[i] / (counts[i] - 1));
        }
    }

    /**
     * Standardize features by removing the mean and scaling to unit variance.
     *
     * @param prob
     */
    public void transform(Problem prob) {
        for (Feature[] fs : prob.x) {
            for (Feature f : fs) {
                int idx = f.getIndex() - 1;
                if (idx != prob.n - 1) { // not the bias
                    if (stds[idx] > minStd) {
                        f.setValue((f.getValue() - means[idx]) / stds[idx]);
                    }
                }
            }
        }
    }
    
    public void transform(Feature[] fs) {
        for (Feature f : fs) {
                int idx = f.getIndex() - 1;
                if (idx != fs.length - 1) { // not the bias
                    if (stds[idx] > minStd) {
                        f.setValue((f.getValue() - means[idx]) / stds[idx]);
                    }
                }
            }
    }

    public void fitTransform(Problem prob) {
        fit(prob);
        transform(prob);
    }

    public final void save(File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        writer.write("#mean\tstd\n");
        for (int i = 0; i < means.length; i++) {
            writer.write(String.format("%f\t%f\n", means[i], stds[i]));
        }
        writer.close();
    }

    public final void load(File file) throws IOException {
        BufferedReader reader = Utility.getReader(file);
        
        // header
        String line = reader.readLine();
        assert line.equals("#mean\tstd") : "No header found";
        
        // data
        ArrayList<Double> meanList = new ArrayList<>();
        ArrayList<Double> stdList = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            meanList.add(Double.parseDouble(fields[0]));
            stdList.add(Double.parseDouble(fields[1]));
        }

        means = new double[meanList.size()];
        stds = new double[meanList.size()];
        for (int i = 0; i < meanList.size(); i++) {
            means[i] = meanList.get(i);
            stds[i] = stdList.get(i);
        }

        reader.close();
    }

    
}
