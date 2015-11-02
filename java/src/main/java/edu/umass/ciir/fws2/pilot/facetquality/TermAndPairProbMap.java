/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws2.pilot.facetquality;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author wkong
 */
public class TermAndPairProbMap {

    HashMap<String, Integer> termIdMap;
    HashMap<String, Double> termProbMap;
    HashMap<String, Double> pairProbMap;

    public TermAndPairProbMap() {
        termIdMap = new HashMap<>();
        termProbMap = new HashMap<>();
        pairProbMap = new HashMap<>();

    }

    public void addTerm(String term) {
        termIdMap.put(term, termIdMap.keySet().size());
    }

    /**
     *
     * @param file
     * @param filter filter by existing termIdMap, or create new termIdMap
     * @throws IOException
     */
    public void loadTermProb(File file, boolean filter) throws IOException {
        if (!filter) {
            termIdMap.clear();
        }

        BufferedReader reader = Utility.getReader(file);
        String line;

        while ((line = reader.readLine()) != null) {
            // 0.00720595275504        -1      BIR_101118      cuttings over its lifetime
            String[] elems = line.split("\t");
            double prob = Double.parseDouble(elems[0]);
            String item = elems[3];

            if (!filter) {
                addTerm(item);
            }

            if (termIdMap.containsKey(item)) {
                termProbMap.put(item, prob);
            }
        }
        reader.close();
    }

    public double getTermProb(String term) {
        return termProbMap.get(term);
    }

    public double getPairProb(String term1, String term2) {
        String id = getItemPairId(term1, term2);
        return pairProbMap.containsKey(id) ? pairProbMap.get(id) : 0.001;
    }

    public void loadTermPairProb(File file) throws IOException {
        BufferedReader reader = Utility.getReader(file);
        String line;

        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            double prob = Double.parseDouble(elems[0]);
            String[] fields = elems[3].split("\\|");
            String item1 = fields[0];
            String item2 = fields[1];
            if (termIdMap.containsKey(item1) && termIdMap.containsKey(item2)) {
                pairProbMap.put(getItemPairId(item1, item2), prob);
            }
        }
        reader.close();
    }

    public String getItemPairId(String item1, String item2) {
        return getItemPairId(termIdMap.get(item1), termIdMap.get(item2));
    }

    public String getItemPairId(int a, int b) {
        return a < b ? a + "_" + b : b + "_" + a;
    }

}
