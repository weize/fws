/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import org.lemurproject.galago.tupleflow.Utility;

/**
 *
 * @author wkong
 */
public class Probablity {

    final static double eps = Utility.epsilon;
    //double prob;
    //double probNeg;
    double log;  // log10( prob)
    double logN; // log10(1-prob)
    double prob;

    public Probablity(double prob) {
        if (prob < eps) {
            prob = eps;
        }
        double probNeg = 1 - prob;
        this.log = Math.log(prob);
        this.logN = Math.log(probNeg);
        this.prob = prob;
    }

    @Override
    public String toString() {
        return "" + this.log;
    }

}
