/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.utility;

import cc.mallet.optimize.Optimizable;

/**
 * Maximizable for 3x^2 - 5x + 2
 * @author wkong
 */
public class SimplePoly implements Optimizable.ByGradientValue {

    double[] params = new double[1];

    @Override
    public void getParameters(double[] doubleArray) {
        doubleArray[0] = params[0];
    }

    @Override
    public int getNumParameters() {
        return 1;
    }

    @Override
    public double getParameter(int n) {
        return params[0];
    }

    @Override
    public void setParameters(double[] doubleArray) {
        params[0] = doubleArray[0];
    }

    @Override
    public void setParameter(int n, double d) {
        params[n] = d;
    }

    @Override
    public double getValue() {
        System.out.println("param = " + params[0] + " value = "
                + (-3 * params[0] * params[0] + 5 * params[0] - 2));

        return -3 * params[0] * params[0] + 5 * params[0] - 2;
    }

    @Override
    public void getValueGradient(double[] buffer) {
        buffer[0] = -6 * params[0] + 5;
    }
}
