/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.utility.Utility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Query Dimension Clustering. See Paper: Zhicheng Dou. Finding Dimensions for
 * Queries. CIKM'11.
 *
 * @author wkong
 */
public class QueryDimensionClusterer {

    boolean debug = false;
    final static int maxListNum = 5000; // only use top maxListNum lists for clustering b/c memory issue
    double distanceMax; // Dia_max in the paper. The threshold for including the list into the cluster. 
    double websiteCountMin; // w_c in the paper. The threshold for a valid cluster.
    double itemRatio;

    public static class QDCluster extends ScoredFacet {

        Integer[] nodeIds;
        public int numOfSite;

        public QDCluster(List<Integer> nodeList) {
            this.nodeIds = nodeList.toArray(new Integer[0]);
        }
    }

    public QueryDimensionClusterer() {

    }

    public QueryDimensionClusterer(Parameters p) {
        this.distanceMax = p.getDouble("qdDistanceMax");
        this.websiteCountMin = p.getDouble("qdWebsiteCountMin");
        this.itemRatio = p.getDouble("qdItemRatio");
        
    }

    public List<ScoredFacet> clusterToFacets(List<FacetFeatures> nodes) {
        return clustersToFacets(cluster(nodes, distanceMax, websiteCountMin), itemRatio);
    }

    /**
     *
     * @param nodes
     * @param distanceMax Dia_max in the paper. The threshold for including the
     * list into the cluster.
     * @param websiteCountMin
     * @return
     */
    public List<QDCluster> cluster(List<FacetFeatures> nodes, double distanceMax, double websiteCountMin) {
        // settings
        this.distanceMax = distanceMax;
        this.websiteCountMin = websiteCountMin;

        // initialization
        nodes = sortSelectNodes(nodes);
        HashSet<Integer> pool = new HashSet<>(); // pool of list ids
        for (int i = 0; i < nodes.size(); i++) {
            pool.add(i);
        }
        ArrayList<QDCluster> clusters = new ArrayList<>();
        HashSet<Integer> candidatePool = new HashSet<>();
        ArrayList<Integer> cluster = new ArrayList<>();
        HashMap<String, Double> distHash = new HashMap<>(); // cache distance between two nodes
        HashMap<Integer, Integer> prevFailedNodesIndex = new HashMap<>();

        // clustering
        for (int i = 0; i < nodes.size(); i++) {
            if (!pool.contains(i)) { // not in the pool
                continue;
            }

            // try to form a new cluster
            cluster.clear();
            cluster.add(i);
            pool.remove(i); // remove from pool;

            candidatePool.clear();
            candidatePool.addAll(pool);

            int maxPreIndex = -1;
            boolean skipFlag = false;

            // Build a candidate cluster for the most important point
            // by iteratively including the point that is closest to the
            // group, until the diameter of the cluster surpasses the distanceMax
            while (true) {
                Object[] res = findClosestNode(cluster, candidatePool, nodes, distHash);
                int closestNodeId = (int) res[0];
                double closestNodeDist = (double) res[1];

                if (closestNodeDist > distanceMax) {
                    break;
                }
                // add into cluster
                cluster.add(closestNodeId);
                candidatePool.remove(closestNodeId);
                pool.remove(closestNodeId);

                if (prevFailedNodesIndex.containsKey(closestNodeId)) { // learn from previous failure
                    int curPreIndex = prevFailedNodesIndex.get(closestNodeId);
                    maxPreIndex = curPreIndex > maxPreIndex ? curPreIndex : maxPreIndex;
                    if (maxPreIndex == cluster.size() - 1) {
                        skipFlag = true;
                        break;
                    }
                }
            }

            // validate cluster
            int siteCount = countWebSite(cluster, nodes);
            if (siteCount >= this.websiteCountMin) {
                // construct cluster
                QDCluster qdCluster = new QDCluster(cluster);
                clusters.add(qdCluster);
            } else {
                // revoke
                for (int id : cluster) {
                    pool.add(id);
                }
                if (!skipFlag) {
                    prevFailedNodesIndex.clear();
                    for (int j = 0; j < cluster.size(); j++) {
                        prevFailedNodesIndex.put(cluster.get(j), j);
                    }
                }
            }
        }

        // ranking clusters and items
        rankClusters(clusters, nodes);
        if (debug) {
            printClusterLists(clusters, nodes);
        }
        rankItems(clusters, nodes);

        return clusters;
    }

    private void rankItems(List<QDCluster> clusters, List<FacetFeatures> nodes) {
        for (QDCluster c : clusters) {
            HashMap<String, HashMap<String, List<Integer>>> itemHash = new HashMap<>();
            int numOfSites = countWebSite(c, nodes);
            c.numOfSite = numOfSites;
            // keyphrase -> site -> ranks
            for (int id : c.nodeIds) {
                FacetFeatures node = nodes.get(id);
                int rank = 0;
                for (String item : node.items) {
                    rank++;
                    if (!itemHash.containsKey(item)) {
                        itemHash.put(item, new HashMap<String, List<Integer>>());
                    }
                    for (String site : node.sites) {
                        if (!itemHash.get(item).containsKey(site)) {
                            itemHash.get(item).put(site, new ArrayList<Integer>());
                        }
                        itemHash.get(item).get(site).add(rank);
                    }
                }
            }

            ArrayList<ScoredItem> items = new ArrayList<>();
            for (String item : itemHash.keySet()) {
                double score = 0;
                for (String site : itemHash.get(item).keySet()) {
                    double avgRankScore = 0;
                    for (int rank : itemHash.get(item).get(site)) {
                        avgRankScore += rank;
                    }
                    avgRankScore /= (double) itemHash.get(item).get(site).size();
                    score += 1.0 / Math.sqrt(avgRankScore);
                }

                items.add(new ScoredItem(item, score));
            }

            Collections.sort(items);
            c.items = items;
        }
    }

    private int countWebSite(QDCluster cluster, List<FacetFeatures> nodes) {
        return countWebSite(Arrays.asList(cluster.nodeIds), nodes);
    }

    private void rankClusters(List<QDCluster> clusters, List<FacetFeatures> nodes) {
        for (QDCluster c : clusters) {
            HashMap<String, Double> siteScore = new HashMap<>();
            for (int id : c.nodeIds) {
                FacetFeatures node = nodes.get(id);
                for (String site : node.sites) {
                    double curScore = node.getScore();
                    if (!siteScore.containsKey(site)) {
                        siteScore.put(site, curScore);
                    } else {
                        double score = siteScore.get(site);
                        if (curScore > score) {
                            siteScore.put(site, curScore);
                        }
                    }
                }
            }

            double cScore = 0;
            for (double s : siteScore.values()) {
                cScore += s;
            }
            c.score = cScore;
        }

        Collections.sort(clusters);
    }

    private int countWebSite(List<Integer> cluster, List<FacetFeatures> nodes) {
        HashSet<String> sites = new HashSet<>();
        for (Integer id : cluster) {
            sites.addAll(Arrays.asList(nodes.get(id).sites));
        }
        return sites.size();
    }

    private Object[] findClosestNode(ArrayList<Integer> cluster, HashSet<Integer> candidatePool, List<FacetFeatures> nodes, HashMap<String, Double> distHash) {
        double closeNodeDist = Double.POSITIVE_INFINITY;
        int closeNodeId = Integer.MAX_VALUE;

        Integer[] candidatePoolNodes = candidatePool.toArray(new Integer[0]);
        for (Integer node : candidatePoolNodes) {
            double distance = distance(cluster, node, nodes, distHash);
            int compare = Utility.compare(distance, closeNodeDist);
            if (compare < 0 || (compare == 0 && node < closeNodeId)) {
                closeNodeDist = distance;
                closeNodeId = node;
            }

            if (distance > this.distanceMax) {
                // these nodes will not be add into the cluster
                // so remove it from candiate pool to save some computation
                candidatePool.remove(node);
            }
        }
        return new Object[]{closeNodeId, closeNodeDist};
    }

    /**
     * distance between a node and a cluster max{node, c}, c in Cluster
     *
     * @param cluster
     * @param node
     * @return
     */
    public double distance(ArrayList<Integer> cluster, int node, List<FacetFeatures> nodes, HashMap<String, Double> distHash) {
        double max = 0;
        for (int n1 : cluster) {
            double distance = distance(n1, node, nodes, distHash);
            if (distance > max) {
                max = distance;
            }
        }
        return max;
    }

    /**
     * distance between two node (by id) cache distance
     *
     * @param n1
     * @param n2
     * @return
     */
    private double distance(int n1, int n2, List<FacetFeatures> nodes, HashMap<String, Double> distHash) {
        String key = n1 > n2 ? n1 + "-" + n2 : n2 + "-" + n1;
        if (!distHash.containsKey(key)) {
            double dist = distance(nodes.get(n1), nodes.get(n2));
            distHash.put(key, dist);
        }
        return distHash.get(key);
    }

    /**
     * calc distance of two nodes
     *
     * @param n1
     * @param n2
     * @return
     */
    public static double distance(FacetFeatures n1, FacetFeatures n2) {
        int overlap = 0;
        int size = Math.min(n1.getItemsLen(), n2.getItemsLen());
        HashSet<String> peers = new HashSet<>();
        peers.addAll(Arrays.asList(n1.items));

        for (String item : n2.items) {
            if (peers.contains(item)) {
                overlap++;
            }
        }
        double distance = 1 - ((double) overlap / (double) size);
        return distance;
    }

    private List<FacetFeatures> sortSelectNodes(List<FacetFeatures> nodes) {
        // sort lists according qdScore
        Collections.sort(nodes);
        // using top maxListNum candidate list
        return nodes.subList(0, Math.min(nodes.size(), maxListNum));
    }

    public void printClusterLists(List<QDCluster> clusters, List<FacetFeatures> nodes) {
        for (QDCluster c : clusters) {
            for (int id : c.nodeIds) {
                FacetFeatures node = nodes.get(id);
                System.err.println(node.getScore() + "\t"
                        + node.itemList
                        + "\t" + node.getFeature(FacetFeatures._sites));
            }
            System.err.println("---------------------------------\n");
        }

    }

    public static List<ScoredFacet> clustersToFacets(List<QDCluster> clusters, double itemRatio) {
        ArrayList<ScoredFacet> facets = new ArrayList<>();
        for (QDCluster c : clusters) {
            double threshold = c.numOfSite * itemRatio;

            ArrayList<ScoredItem> items = new ArrayList<>();
            for (ScoredItem scoredItem : c.items) {
                if (scoredItem.score > 1 && scoredItem.score > threshold) {
                    items.add(scoredItem);
                }
            }

            if (items.size() > 0) {
                facets.add(new ScoredFacet(items, c.score));
            }

        }
        return facets;
    }
}
