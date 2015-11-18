/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.demo.search.GalagoSearchEngine;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class QdFacetFeatureOnlineExtractor extends QdFacetFeatureExtractor{
    GalagoSearchEngine galago;
    boolean extractDf;
    double clueCdf;
    
    public QdFacetFeatureOnlineExtractor(Parameters p, GalagoSearchEngine galago) {
        clueCdf = p.getLong("clueCdf");
        extractDf = p.getBoolean("clueDocFreq");
        this.galago = galago;
    }
    

    @Override
    protected double getDf(String term) {
        if (!extractDf) return clueCdf / 10000;
        long df = galago.getDocFreq(term);
        return df == -1 ? 0 : df;        
    }

    @Override
    protected double getCdf() {
        return clueCdf;
    }
    
}
