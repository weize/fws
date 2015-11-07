/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.demo.search;

import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class SearchEngineFactory {

    public static SearchEngine instance(Parameters p) {
        String searchEngine = p.getString("search-engine");
        if (searchEngine.equalsIgnoreCase("galago")) {
            return new GalagoSearchEngine(p);
        } else if(searchEngine.equalsIgnoreCase("bing")) {
            //return new BingSearchEngine(p);
            return null;
        } else {
            throw new RuntimeException("Do not recongize search engine: " + searchEngine);
        }
    }
    
}
