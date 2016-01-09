/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.utility;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author wkong
 */
public class Instance {

    static boolean debug = false;

    String qid;
    double[][] tFeatures; // term features[termIndex][featureIndex]
    double[][] pFeatures; // pair features[pairIndex][featureIndex]
    boolean[] Ys; // labels for y_i  (if term_i is postive
    boolean[] Zs; // label for z_{i,j} (if term_i and term_j are facet terms and in the same cluster)
    // postive term have two types of ids
    // termId : term index (as used in tFeatures
    // pos Id : index the positive terms
    Integer[] posIdToTid; // posIdToTid[posId] == termIndex
    int nPosTerms; // pos term: 0 to nPosTerms -1
    int nT; // number of all terms
    int nP; // number of all pairs

    public static Instance[] readInstances(File tData, File pData, List<Integer> tfIndices, List<Integer> pfIndices) throws IOException {
        // read tData

        HashMap<String, List<String>> tDataGroups = groupDataByQid(tData);
        HashMap<String, List<String>> pDataGroups = groupDataByQid(pData);

        List<Instance> instanceList = new ArrayList<>(tDataGroups.size());
        for (String qid : tDataGroups.keySet()) {
            if (debug) {
                System.err.println("qid=" + qid);
            }
            // load term 0 to term n-1
            List<String> tLines = tDataGroups.get(qid);
            int nTerms = tLines.size();
            Instance instance = new Instance();
            instance.tFeatures = new double[nTerms][]; // # terms
            instance.Ys = new boolean[nTerms];

            instance.nT = nTerms;
            HashMap<String, Integer> posTermIdMap = new HashMap<>(); // posTerm -> posId
            ArrayList<Integer> posIdToTidList = new ArrayList<>();
            for (int i = 0; i < tLines.size(); i++) {
                LabelFeatures lf = new LabelFeatures(tLines.get(i), tfIndices);
                instance.tFeatures[i] = lf.features;
                instance.Ys[i] = lf.label > 0; // -1 or +1
                if (instance.Ys[i]) {
                    posIdToTidList.add(i); // posId -> termId
                    posTermIdMap.put(lf.name, posTermIdMap.size()); // term -> posId
                }
            }
            instance.posIdToTid = posIdToTidList.toArray(new Integer[0]);
            instance.nPosTerms = instance.posIdToTid.length; // number of positive terms

            if (Instance.debug) {
                System.err.println("nPosTerms = " + instance.nPosTerms);
            }
            // load pair
            List<String> pLines = pDataGroups.get(qid);
            int nPairs = pLines.size();
            instance.nP = nPairs;
            instance.pFeatures = new double[nPairs][];
            instance.Zs = new boolean[nPairs];

            if (debug) {
                System.err.println("nPairs=" + nPairs);
            }
            for (String line : pLines) {
                LabelFeatures lf = new LabelFeatures(line, pfIndices);
                String[] terms = lf.name.split("\\|");

                // pairs not appear in term files
                // some postive terms in anntation files
                // are not found in candidate lists, therefore not found in feature file
                // simply ignore this for now
                int pid = instance.getPid(posTermIdMap.get(terms[0]), posTermIdMap.get(terms[1]));
                instance.pFeatures[pid] = lf.features;
                instance.Zs[pid] = lf.label > 0;

            }

            instanceList.add(instance);
        }

        return instanceList.toArray(new Instance[0]);
    }

    private static HashMap<String, List<String>> groupDataByQid(File data) throws IOException {
        BufferedReader reader = Utility.getReader(data);
        HashMap<String, List<String>> dataGroups = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            // label feature1 feature2 ... feature_n #label qid term
            String[] dataComment = line.split("#", 2);
            String comment = dataComment[1].trim();
            String qid = comment.split("\t")[1];
            if (!dataGroups.containsKey(qid)) {
                dataGroups.put(qid, new ArrayList<String>());
            }
            dataGroups.get(qid).add(line);
        }
        reader.close();
        return dataGroups;
    }

    /**
     * This encode two pos term id into pair id, which start from 0 to
     * nPosTerms*(nPosTerms-1) -1.
     *
     * for example: nPosTerms = 4 <br>
     * (small, big, pid) <br>
     * 0, 1, 0 <br>
     * 0, 2, 1 <br>
     * 0, 3, 2 <br>
     * 1, 2, 3 <br>
     * 1, 3, 4 <br>
     * 2, 3, 5 <br>
     *
     * @param posId1
     * @param posId2
     * @return
     */
    public int getPid(int posId1, int posId2) {
        if (posId1 < posId2) {
            return getPidWithSmallBigPosIds(posId1, posId2);
        } else {
            return getPidWithSmallBigPosIds(posId2, posId1);
        }
    }

    public int getPidWithSmallBigPosIds(int small, int big) {
        if (debug) {
            System.err.println("small= " + small + "\tbig= " + big);
        }
        return (2 * nPosTerms - small - 1) * small / 2 + big - small - 1;
    }

    public static class LabelFeatures {

        final static double bias = 1.0;
        String name;
        double label;
        double[] features;

        public LabelFeatures(String line, List<Integer> fIndices) {
            String[] dataComment = line.split("#", 2);
            String comment = dataComment[1].trim();
            name = comment.split("\t")[2];
            String dataStr = dataComment[0].trim();
            String[] fields = dataStr.split("\t");

            features = new double[fIndices.size() + 1]; // one additional for bias
            label = Double.parseDouble(fields[0]);
            for (int i = 0; i < fIndices.size(); i++) {
                features[i] = Double.parseDouble(fields[fIndices.get(i)]);
            }
            features[features.length - 1] = bias;
        }
    }

}
