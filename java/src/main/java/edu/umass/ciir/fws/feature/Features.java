/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.utility.TextProcessing;

/**
 *
 * @author wkong
 */
public class Features {

    protected Object[] features;

    public void setFeature(Object value, int idx) {
        this.features[idx] = value;
    }

    public Object getFeature(int idx) {
        return this.features[idx];
    }

    public String featuresToString() {
        return TextProcessing.join(features, "\t");
    }
    
    public int getSize() {
        return features.length;
    }
}
