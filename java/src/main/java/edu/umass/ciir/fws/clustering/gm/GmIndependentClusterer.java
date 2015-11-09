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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class GmIndependentClusterer extends GraphicalModelClusterer {

    // thresholds 
    final boolean debug = false;
    double termProbTh;
    double pairProbTh;
    // for clustering
    HashSet<Integer> pool;
    ArrayList<Integer> cluster;
    ArrayList<GmCluster> clusters;
    double closeNodeSim;
    int closeNodeId;

    public GmIndependentClusterer(double termProbTh, double pairProbTh) {
        this.termProbTh = termProbTh;
        this.pairProbTh = pairProbTh;
    }

    public GmIndependentClusterer(Parameters p) {
        this.termProbTh = p.getDouble("termProbTh");
        this.pairProbTh = p.getDouble("pairProbTh");
    }

    @Override
    public List<ScoredFacet> cluster(File termPredictFile, File termPairPredictFile) throws IOException {
        loadItems(termPredictFile);
        loadItemPairs(termPairPredictFile);
        cluster();
        rankClusters();

        // convert GmCluster to ScoredFacet
        return convertClustersToFacets(clusters);
    }

    @Override
    public List<ScoredFacet> cluster(List<ScoredProbItem> items, HashMap<String, Integer> itemIdMap, HashMap<String, Probability> pairProbs) {
        this.items = items;
        this.itemIdMap = itemIdMap;
        this.pairProbs = pairProbs;
        cluster();
        rankClusters();
        return convertClustersToFacets(clusters);
    }

    private void rankClusters() {
        for (GmCluster c : clusters) {
            c.score = 0;
            c.items = new ArrayList<>();

            for (Integer nodeId : c.nodeIds) {
                ScoredProbItem item = items.get(nodeId);
                c.score += item.prob.prob;
                c.items.add((ScoredItem) item);
            }
            Collections.sort(c.items);
        }

        Collections.sort(clusters);
    }

    private void cluster() {
        // initialize pool
        pool = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).prob.prob >= termProbTh) {
                pool.add(i);
            }
        }

        // clusters saved
        clusters = new ArrayList<>();
        cluster = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (!pool.contains(i)) {
                continue;
            }

            if (debug) {
                System.out.println("processing " + items.get(i));
            }

            // try to form a new cluster
            cluster.clear();
            cluster.add(i);
            pool.remove(i);

            while (true) {
                findCloseNode(cluster, pool);
                if (debug) {
                    if (closeNodeId != -1) {
                        System.out.println("> " + items.get(closeNodeId) + "\t" + closeNodeSim);
                    }
                }

                if (closeNodeSim < pairProbTh) {
                    break;
                }
                // add to the cluster
                cluster.add(closeNodeId);
                pool.remove(closeNodeId);
                if (debug) {
                    System.out.println("add");
                }
            }

            if (debug) {
                System.out.print("done: ");
                for (int node : cluster) {
                    System.out.print(items.get(node).item + "|");
                }
                System.out.println();
            }
            clusters.add(new GmCluster(cluster));
        }

    }

    private void findCloseNode(ArrayList<Integer> cluster, HashSet<Integer> pool) {
        this.closeNodeSim = -1;
        this.closeNodeId = -1;

        for (Integer node : pool) {
            double sim = sim(cluster, node);
            if (sim > closeNodeSim
                    || (Math.abs(sim - closeNodeSim) < eps && items.get(node).prob.prob > items.get(closeNodeId).prob.prob)) {
                closeNodeSim = sim;
                closeNodeId = node;
            }
        }
    }

    private double sim(ArrayList<Integer> cluster, Integer node) {
        double min = Double.MAX_VALUE;
        for (Integer i : cluster) {
            double sim = prob(i, node);
            if (sim < min) {
                min = sim;
            }
        }
        return min;
    }

    private List<ScoredFacet> convertClustersToFacets(ArrayList<GmCluster> clusters) {
        ArrayList<ScoredFacet> outClusters = new ArrayList<>();
        for (GmCluster cluster : clusters) {
            outClusters.add(cluster);
        }
        return outClusters;
    }

}
