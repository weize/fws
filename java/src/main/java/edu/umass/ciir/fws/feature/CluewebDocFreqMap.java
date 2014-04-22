/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * A map that stores doc frequence for terms in clueweb.
 * @author wkong
 */
public class CluewebDocFreqMap {
    
    HashMap<String, Double> termDf; // term -> docFreq
    
    public CluewebDocFreqMap(String clueDfFile) throws IOException {
        termDf = new HashMap<>();
        loadClueWebDocFreqs(clueDfFile);
    }
    
    
    private void loadClueWebDocFreqs(String fileName) throws IOException {
        termDf.clear();
        BufferedReader reader = Utility.getReader(fileName);
        String line;
        while ((line = reader.readLine()) != null) {
            // <term> \t <df>
            String[] elems = line.split("\t");
            double df = Double.parseDouble(elems[1]);
            termDf.put(elems[0], df);
        }
        reader.close();
    }
    
    public double getDf(String term) {
        return termDf.containsKey(term) ? termDf.get(term) : 1.0;
    }
}
