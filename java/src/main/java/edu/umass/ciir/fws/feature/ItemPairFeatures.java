/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.feature;


/**
 *
 * @author wkong
 */
public class ItemPairFeatures extends Features{
    public String item1;
    public String item2;
    
    public static final int _lenDiff = 0;
    public static final int _listFreq = 1;
    public static final int _contextListSim = 2;
    public static final int _contextTextSim = 3;
    public static final int size = 4;

    public ItemPairFeatures() {
    }

    public ItemPairFeatures(String item1, String item2) {
        this.item1 = item1;
        this.item2 = item2;
        this.features = new Object[size];
    }

    public String itemPairToString() {
        return item1 + "|" + item2;
    }

    @Override
    public String toString() {
        return itemPairToString() + "\t" + featuresToString();
    }

    public void incFeature(int idx) {
        features[idx] = (Integer) features[idx] + 1;
    }
    
}
