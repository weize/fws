/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.utility.TextProcessing;
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
public class GmCoordinateAscentClusterer extends GraphicalModelClusterer {

    // for clustering
    HashSet<Integer> pool; // kp pool
    ArrayList<Integer> cluster;
    ArrayList<ArrayList<Integer>> clusters;
    double closeNodeSim;
    int closeNodeId;
    ArrayList<ScoredFacet> outClusters;
    int bestCluster;
    int iterMaxNum = 10;
    double threshold = 0;
    final static boolean debug = false;

    int YPosInitalSize = 200;
    int maxSStep = 10;

    HashMap<Integer, Boolean> Y; // y_i = 1: term i is a facet term
    HashMap<String, Boolean> Z; // Z_i,j = 1: term pair i, j is a facet term pair

    public GmCoordinateAscentClusterer() {
        Y = new HashMap<>();
        Z = new HashMap<>();
    }

    @Override
    public List<ScoredFacet> cluster(File termPredictFile, File termPairPredictFile) throws IOException {
        loadItems(termPredictFile);
        loadItemPairs(termPairPredictFile);
        cluster();
        induce();
        rankCluster();
        return outClusters;
    }

    private void cluster() {
        // initialize Y
        pool = new HashSet<>();
        Y.clear();
        for (int i = 0; i < items.size(); i++) {
            if (i < YPosInitalSize) {
                Y.put(i, true);
            } else {
                Y.put(i, false);
            }
        }

        for (int i = 0; i < iterMaxNum; i++) {
            clusterStep(i);
        }
    }

    private void rankCluster() {
        outClusters = new ArrayList<>();
        for (ArrayList<Integer> c : clusters) {
            ArrayList<ScoredItem> curItems = new ArrayList<>();
            double score = 0;
            for (int t : c) {

                ScoredProbItem item = items.get(t);
                curItems.add(item);
                score += item.prob.prob;

            }
            if (curItems.size() > 1) {
                Collections.sort(curItems);
                ScoredFacet curCluster = new ScoredFacet(curItems, score);
                outClusters.add(curCluster);
            }
        }

        Collections.sort(outClusters);
    }

    private double loglikelihood() {
        double ll = 0;
        for (int i = 0; i < items.size(); i++) {
            if (Y.get(i)) {
                ll += logProb(i);
            } else {
                ll += logNProb(i);
            }
        }

        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                // Z_{i,j}
                if (Y.get(i) && Y.get(j)) {
                    if (Z.get(getItemPairId(i, j))) {
                        ll += logProb(i, j);
                    } else {
                        ll += logNProb(i, j);
                    }
                } else {
                    //ll += logNProb(i, j);
                }

            }
        }

        return ll;
    }

    private void induce() {
        clusters = new ArrayList<>();
        // find maximal cliques
        for (int i = 0; i < items.size(); i++) {
            ArrayList<Integer> c = new ArrayList<>();
            c.add(i);
            if (Y.get(i)) {
                for (int j = i + 1; j < items.size(); j++) {
                    if (allConnected(c, j)) {
                        c.add(j);
                    }
                }
                //check if this is maximal
                boolean couldBeLarger = false;
                for (int j = 0; j < i; j++) {
                    if (allConnected(c, j)) {
                        couldBeLarger = true;
                    }
                }
                if (!couldBeLarger) {
                    clusters.add(c);
                    //System.out.println("create cluster size = " + c.size());
                }
            }
        }
    }

    private boolean allConnected(ArrayList<Integer> c, int j) {
        for (int i : c) {
            if (!Z.get(getItemPairId(i, j))) {
                return false;
            }
        }
        return true;

    }

    private void llReport(int step, int sstep) {
        double ll = loglikelihood();
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            if (Y.get(i)) {
                count++;
            }
        }
        //System.out.println(String.format("step %d/%d\tll = %.4f count = %d/%d", step, sstep, ll, count, items.size()));
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

    private void clusterStep(int step) {
        // fix Y, and optimaize over Z
        //Z.clear();
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                // Z_{i,j}
                if (Y.get(i) && Y.get(j)) {
                    // Z_i,j = argmax lgP(Z_i,j)
                    if (logProb(i, j) > logNProb(i, j)) {
                        Z.put(getItemPairId(i, j), true);
                        //System.out.println(String.format("SP prob = %.4f [%s, %s]", prob(i, j), items.get(i).item, items.get(j).item));
                    } else {
                        Z.put(getItemPairId(i, j), false);
                        //System.out.println(String.format("DP prob = %.4f [%s, %s]", prob(i, j), items.get(i).item, items.get(j).item));
                    }
                } else {
                    // y_i ==0 or Y_j==0 -> Z_j,j = 0
                    Z.put(getItemPairId(i, j), false);
                    //System.out.println(String.format("DPD prob = %.4f [%s, %s]", prob(i, j), items.get(i).item, items.get(j).item));
                }
            }
        }

        // fix Z, and optimaize over Y
        // Y.clear();
        HashSet<Integer> YMustPos = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                if (Z.get(getItemPairId(i, j))) {
                    // Z_j,j == 1 -> // y_i ==1 && Y_j==1
                    Y.put(i, true);
                    Y.put(j, true);
                    YMustPos.add(i);
                    YMustPos.add(j);
                }
            }
        }

        double[] Delta = new double[items.size()];
        ArrayList<Integer> Yleft = new ArrayList<>();
        //System.out.println("YMustPos size = " + YMustPos.size());

        // delta_i < 0 -> y_i = 0
        for (int i = 0; i < items.size(); i++) {
            if (!YMustPos.contains(i)) {
                // delta = lgP(Y_i = 1) - lgP(Y_i = 0) + sum_j \in {y_j| j_j == 1} {logP(Z_i,j =0)}
                double delta = logProb(i) - logNProb(i);
                for (Integer j : YMustPos) {
                    delta += logNProb(i, j);
                }

                if (delta <= 0) {
                    Y.put(i, false);
                } else {
                    Yleft.add(i);
                }
                Delta[i] = delta;
            }
        }

        //System.out.println(TextProcessing.join(Delta, "\n"));
        //System.out.println("Yleft size = " + Yleft.size());

//        for (int i = 0; i < items.size(); i++) {
//            if (!Y.containsKey(i)) {
//                Y.put(i, true);
//            }
//        }
        double llLast = 0;
        for (int sstep = 0; sstep < maxSStep; sstep++) {
            Collections.shuffle(Yleft);
            for (int i : Yleft) {
                double delta = logProb(i) - logNProb(i);
                for (int j = 0; j < items.size(); j++) {
                    if (j != i) {
                        if (Y.get(j)) {
                            delta += logNProb(i, j);
                        }
                    }
                }
                //optimize over one Y_i
                if (delta > 0) {
                    Y.put(i, true);
                } else {
                    Y.put(i, false);
                }

//                int count = 0;
//                for (int k = 0; k < items.size(); k++) {
//                    if (Y.get(k)) {
//                        count++;
//                    }
//                }
//                double ll = loglikelihood();
//                double llDiff = ll - llLast;
//                llLast = ll;
                //System.out.println(String.format("step = %d/%d\tll = %.4f\tdelta = %.4f\tll_diff = %.4f\tcount = %d/%d", step, sstep, ll, delta, llDiff, count, items.size()));

            }
            llReport(step, sstep);
        }

//        for (int i = 0; i < items.size(); i++) {
//            for (int j = i + 1; j < items.size(); j++) {
//                if (Z.get(getItemPairId(i, j))) {
//                    System.out.println(String.format("PY prob = %.4f [%s, %s]", prob(i, j), items.get(i).item, items.get(j).item));
//                }
//            }
//        }
    }


}
