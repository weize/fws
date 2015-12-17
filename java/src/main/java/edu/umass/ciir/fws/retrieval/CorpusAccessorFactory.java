/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.retrieval;

import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class CorpusAccessorFactory {
    
    public static CorpusAccessor instance(Parameters p) throws Exception {
        String system = p.getString("system");
        switch (system) {
            case "galago":
                return new GalagoCorpusAccessor(p);
            case "bing":
                return new LocalCorpusAccessor(p);
            default:
                return null;
        }
    }
    
}
