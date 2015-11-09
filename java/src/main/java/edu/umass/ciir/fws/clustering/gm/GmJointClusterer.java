/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author wkong
 */
public class GmJointClusterer extends GraphicalModelClusterer {

    // for clustering
    HashSet<Integer> pool; // kp pool
    ArrayList<Integer> cluster;
    ArrayList<ArrayList<Integer>> clusters;
    double closeNodeSim;
    int closeNodeId;
    ArrayList<ScoredFacet> outClusters;
    int bestCluster;
    int iterMaxNum = 2;
    double threshold = 0;
    final static boolean debug = false;

    public GmJointClusterer() {

    }

    @Override
    public List<ScoredFacet> cluster(File termPredictFile, File termPairPredictFile) throws IOException {
        loadItems(termPredictFile);
        loadItemPairs(termPairPredictFile);
        cluster();
        rankClusters();
        return outClusters;
    }
    
    @Override
    public List<ScoredFacet> cluster(List<ScoredProbItem> items, HashMap<String, Integer> itemIdMap, HashMap<String, Probability> pairProbs) {
        this.items = items;
        this.itemIdMap = itemIdMap;
        this.pairProbs = pairProbs;
        cluster();
        rankClusters();
        return outClusters;
    }

    private void cluster() {
        // initialize pool
        pool = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            pool.add(i);
        }

        for (int i = 0; i < iterMaxNum; i++) {
            clusterStep();
        }
    }

    private void rankClusters() {
        outClusters = new ArrayList<>();
        for (ArrayList<Integer> c : clusters) {
            ArrayList<ScoredItem> curItems = new ArrayList<>();
            double score = 0;
            for (int t : c) {
                if (pool.contains(t)) {
                    ScoredProbItem item = items.get(t);
                    curItems.add(item);
                    score += item.prob.prob;
                }
            }
            if (curItems.size() > 1) {
                Collections.sort(curItems);
                ScoredFacet curCluster = new ScoredFacet(curItems, score);
                outClusters.add(curCluster);
            }
        }

        Collections.sort(outClusters);
    }

 

    

    private static class ItemComparator implements Comparator<Integer> {

        List<ScoredProbItem> items;

        public ItemComparator(List<ScoredProbItem> items) {
            this.items = items;
        }

        @Override
        public int compare(Integer t1, Integer t2) {
            return Double.compare(items.get(t2).prob.prob, items.get(t1).prob.prob);
        }
    }

    private void clusterStep() {
        Integer[] curItems = pool.toArray(new Integer[0]);
        Arrays.sort(curItems, new ItemComparator(items));

        clusters = new ArrayList<>();
        // clustering
        for (Integer item : curItems) {
            double bestScore = findBestCluster(item);

            if (bestScore < 0) { // create singleton for kp
                ArrayList<Integer> c = new ArrayList<>();
                c.add(item);
                clusters.add(c);
            } else { // add to bestCluter
                clusters.get(bestCluster).add(item);
            }
        }

        // remove outliers
        for (ArrayList<Integer> c : clusters) {
            if (debug) {
                for (int id : c) {
                    System.out.print(items.get(id).item + "|");
                }
                System.out.println();
            }
            // for each cluster
            ArrayList<Integer> keep = new ArrayList<>();
            for (int t : c) { // check if should remove this kp
                double score = logProb(t) - logNProb(t);
                // intra
                for (int t2 : keep) {
                    if (t != t2) {
                        score += logProb(t, t2);
                    }
                }

                if (debug) {
                    System.err.println("score " + score + " " + items.get(t).item);
                }
                if (score < -threshold || c.size() == 1) {
                    pool.remove(t);
                    if (debug) {
                        System.err.println("remove " + items.get(t).item);
                    }
                } else {
                    keep.add(t);
                }
            }
        }
    }

    private double findBestCluster(int itemId) {
        double bestScore = Double.NEGATIVE_INFINITY;
        this.bestCluster = -1;
        for (int i = 0; i < clusters.size(); i++) {
            double score = 0;
            for (int itemId2 : clusters.get(i)) {
                score += logProb(itemId, itemId2) - logNProb(itemId, itemId2);
            }

            if (score > bestScore) {
                bestScore = score;
                this.bestCluster = i;
            }
        }
        return bestScore;
    }

}
