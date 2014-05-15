/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.ScoredItem;

/**
 *
 * @author wkong
 */
public class ScoredProbItem extends ScoredItem {

    Probablity prob;

    public ScoredProbItem(String item, double probablity) {
        super(item, probablity);
        prob = new Probablity(probablity);
    }

    public double logProb() {
        return prob.log;
    }

    public double logNProb() {
        return prob.logN;
    }
}
