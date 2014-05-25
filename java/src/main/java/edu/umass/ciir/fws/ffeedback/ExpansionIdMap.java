/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

/**
 * keep track of expansions, assign unique id for each expansions.
 *
 * @author wkong
 */
public class ExpansionIdMap {

    TreeMap<String, TreeMap<String, Integer>> idMap; // qid->term-> id

    public ExpansionIdMap(File file) throws IOException {
        load(file);
    }

    public ExpansionIdMap() {
        idMap = new TreeMap<>();
    }

    private void load(File file) throws IOException {
        idMap = new TreeMap<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            String qid = elems[0];
            String term = elems[1];
            Integer id = Integer.parseInt(elems[2]);
            put(qid, term, id);
        }
        reader.close();
    }

    public void output(File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (String qid : idMap.keySet()) {
            TreeMap<String, Integer> map = idMap.get(qid);
            for (String term : map.keySet()) {
                writer.write(String.format("%s\t%s\t%d\n", qid, term, map.get(term)));
            }
        }
        writer.close();
    }

    private void put(String qid, String term, Integer id) {
        if (!idMap.containsKey(qid)) {
            idMap.put(qid, new TreeMap<String, Integer>());
        }
        idMap.get(qid).put(term, id);
    }

    public boolean contains(String qid, String term) {
        return idMap.containsKey(qid) && idMap.get(qid).containsKey(term);
    }

    public Integer getId(String qid, String term) {
        if (contains(qid, term)) {
            return idMap.get(qid).get(term);
        } else {
            return add(qid, term);
        }
    }

    private Integer add(String qid, String term) {
        if (!idMap.containsKey(qid)) {
            idMap.put(qid, new TreeMap<String, Integer>());
        }

        TreeMap<String, Integer> map = idMap.get(qid);
        int id = map.size();
        map.put(term, id);
        return id;
    }

}
