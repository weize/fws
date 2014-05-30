/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Query Dimension Clustering. See Paper: Zhicheng Dou. Finding Dimensions for
 * Queries. CIKM'11.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class QueryDimensionClusterers implements Processor<TfQueryParameters> {

    boolean debug = false;
    // lists
    List<FacetFeatures> nodes; // html lists featuress    

    HashSet<Integer> pool; // pool of list ids
    HashSet<Integer> candidatePool;
    ArrayList<QDCluster> clusters;
    ArrayList<Integer> cluster;
    HashMap<Integer, Integer> prevFailedNodesIndex;
    double closeNodeDist;
    int closeNodeId;
    HashMap<String, Double> distHash; // cache distance between two nodes
    double distanceMax; // Dia_max in the paper. The threshold for including the list into the cluster. 
    double websiteCountMin; // w_c in the paper. The threshold for a valid cluster.
    String clusterDir;
    String featureDir;
    final static int maxListNum = 5000; // only use top maxListNum lists for clustering b/c memory issue

    String qid;

    public QueryDimensionClusterers(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String runDir = p.getString("qdRunDir");
        featureDir = Utility.getFileName(runDir, "feature");
        clusterDir = Utility.getFileName(runDir, "cluster");

        clusters = new ArrayList<>();
        cluster = new ArrayList<>();
        pool = new HashSet<>();
        candidatePool = new HashSet<>();
        prevFailedNodesIndex = new HashMap<>();
        distHash = new HashMap<>();

    }

    @Override
    public void process(TfQueryParameters queryParameters) throws IOException {
        //setQueryParameters
        System.err.println(String.format("Processing qid:%s parameters:%s", queryParameters.id, queryParameters.parameters));
        qid = queryParameters.id;
        String[] fields = Utility.splitParameters(queryParameters.parameters);
        distanceMax = Double.parseDouble(fields[0]);
        websiteCountMin = Double.parseDouble(fields[1]);

        loadFacetFeatures(); // loadClusters lists and features
        initializeClusetering();
        clustering();
        rankClusters();
        if (debug) {
            printClusterLists();
        }
        rankItems();
        output();
    }

    private void clustering() {
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
            while (true) {
                findCloseNode(cluster, candidatePool);
                if (this.closeNodeDist > this.distanceMax) {
                    break;
                }
                // add into cluster
                cluster.add(this.closeNodeId);
                candidatePool.remove(this.closeNodeId);
                pool.remove(this.closeNodeId);

                if (prevFailedNodesIndex.containsKey(this.closeNodeId)) { // learn from previous failure
                    int curPreIndex = prevFailedNodesIndex.get(closeNodeId);
                    maxPreIndex = curPreIndex > maxPreIndex ? curPreIndex : maxPreIndex;
                    if (maxPreIndex == cluster.size() - 1) {
                        skipFlag = true;
                        break;
                    }
                }
            }

            // validate cluster
            int siteCount = countWebSite(cluster);
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
    }

    public void printClusterLists() {
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

    private int countWebSite(List<Integer> cluster) {
        HashSet<String> sites = new HashSet<>();
        for (Integer id : cluster) {
            sites.addAll(Arrays.asList(nodes.get(id).sites));
        }
        return sites.size();
    }

    private int countWebSite(QDCluster cluster) {
        return countWebSite(Arrays.asList(cluster.nodeIds));
    }

    private void findCloseNode(ArrayList<Integer> cluster, HashSet<Integer> candidatePool) {
        this.closeNodeDist = Double.POSITIVE_INFINITY;
        this.closeNodeId = Integer.MAX_VALUE;

        Integer[] candidatePoolNodes = candidatePool.toArray(new Integer[0]);
        for (Integer node : candidatePoolNodes) {
            double distance = distance(cluster, node);
            int compare = Utility.compare(distance, closeNodeDist);
            if (compare < 0 || (compare == 0 && node < closeNodeId)) {
                closeNodeDist = distance;
                closeNodeId = node;
            }

            if (distance > this.distanceMax) {
                candidatePool.remove(node);
            }
        }
    }

    /**
     * distance between a node and a cluster max{node, c}, c in Cluster
     *
     * @param cluster
     * @param node
     * @return
     */
    public double distance(ArrayList<Integer> cluster, int node) {
        double max = 0;
        for (int n1 : cluster) {
            double distance = distance(n1, node);
            if (distance > max) {
                max = distance;
            }
        }
        return max;
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

    /**
     * distance between two node (by id) cache distance
     *
     * @param n1
     * @param n2
     * @return
     */
    private double distance(int n1, int n2) {
        String key = n1 > n2 ? n1 + "-" + n2 : n2 + "-" + n1;
        if (!distHash.containsKey(key)) {
            double dist = distance(nodes.get(n1), nodes.get(n2));
            distHash.put(key, dist);
        }
        return distHash.get(key);
    }

    private void loadFacetFeatures() throws IOException {
        String facetFeaturesFile = Utility.getQdFacetFeatureFileName(featureDir, qid);
        nodes = FacetFeatures.readFromFile(facetFeaturesFile);
        // sort lists according qdScore
        Collections.sort(nodes);
        // using top maxListNum candidate list
        nodes = nodes.subList(0, Math.min(nodes.size(), maxListNum));
    }

    private void rankClusters() {
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

    private void rankItems() {
        for (QDCluster c : clusters) {
            HashMap<String, HashMap<String, List<Integer>>> itemHash = new HashMap<>();
            int numOfSites = countWebSite(c);
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

    private void output() throws IOException {
        String fileName = Utility.getQdClusterFileName(clusterDir, qid, distanceMax, websiteCountMin);
        Utility.createDirectoryForFile(fileName);
        Writer writer = Utility.getWriter(fileName);
        for (QDCluster c : clusters) {
            if (c.items.size() < 2) {
                continue;
            }
            writer.write(String.format("%s\t%s\t%s\n", c.score, c.numOfSite, TextProcessing.join(c.items, "|")));
        }
        writer.close();
        System.err.println(String.format("Written in %s", fileName));
    }

    @Override
    public void close() throws IOException {

    }

    private void initializeClusetering() {
        // set pool flag
        int len = nodes.size();
        pool.clear();
        for (int i = 0; i < len; i++) {
            pool.add(i);
        }
        clusters.clear();
        distHash.clear();
        prevFailedNodesIndex.clear();
    }

    class QDCluster extends ScoredFacet {

        Integer[] nodeIds;
        int numOfSite;

        public QDCluster(List<Integer> nodeList) {
            this.nodeIds = nodeList.toArray(new Integer[0]);
        }
    }
}
