/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author wkong
 */
public abstract class GraphicalModelClusterer {

    protected final static double eps = Utility.epsilon;
    public final static int numTopScoredItems = TermPairDataExtractor.numTopScoredItems;

    List<ScoredProbItem> items;
    HashMap<String, Integer> itemIdMap;
    HashMap<String, Probability> pairProbs;

    public abstract List<ScoredFacet> cluster(File termPredictFile, File termPairPredictFile) throws IOException;

    public abstract List<ScoredFacet> cluster(List<ScoredProbItem> items, HashMap<String, Integer> itemIdMap, HashMap<String, Probability> pairProbs);

    public void loadItems(File predictFile) throws IOException {
        BufferedReader reader = Utility.getReader(predictFile);
        String line;
        ArrayList<ScoredProbItem> allItems = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            // 0.00720595275504        -1      BIR_101118      cuttings over its lifetime
            String[] elems = line.split("\t");
            double score = Double.parseDouble(elems[0]);
            String item = elems[3];
            allItems.add(new ScoredProbItem(item, score));
        }
        reader.close();
        Collections.sort(allItems);
        items = allItems.subList(0, Math.min(numTopScoredItems, allItems.size()));

        // item -> id
        itemIdMap = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            itemIdMap.put(items.get(i).item, i);
        }
    }

    public void loadItemPairs(File file) throws IOException {
        pairProbs = new HashMap<>();

        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            double prob = Double.parseDouble(elems[0]);
            String[] fields = elems[3].split("\\|");
            String item1 = fields[0];
            String item2 = fields[1];
            if (itemIdMap.containsKey(item1) && itemIdMap.containsKey(item2)) {
                pairProbs.put(getItemPairId(item1, item2), new Probability(prob));
            }

        }
        reader.close();
    }

    public String getItemPairId(String item1, String item2) {
        return getItemPairId(itemIdMap.get(item1), itemIdMap.get(item2));
    }

    public String getItemPairId(int a, int b) {
        return a < b ? a + "_" + b : b + "_" + a;
    }

    /**
     * P(item1, item2 are a facet term pair)
     *
     * @param item1
     * @param item2
     * @return
     */
    public double prob(int item1, int item2) {
        String pid = getItemPairId(item1, item2);
        return pairProbs.containsKey(pid) ? pairProbs.get(pid).prob : 0;
    }

    public double logProb(int itemId) {
        return items.get(itemId).prob.log;
    }

    public double logNProb(int itemId) {
        return items.get(itemId).prob.logN;
    }

    public double logProb(int itemId1, int itemId2) {
        String pid = getItemPairId(itemId1, itemId2);
        return pairProbs.containsKey(pid) ? pairProbs.get(pid).log : Double.NEGATIVE_INFINITY;
    }

    public double logNProb(int itemId1, int itemId2) {
        String pid = getItemPairId(itemId1, itemId2);
        return pairProbs.containsKey(pid) ? pairProbs.get(pid).logN : 0;
    }
}
