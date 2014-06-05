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

    TreeMap<String, TreeMap<String, TreeMap<String, Long>>> idMap; // qid->model->exp-> id

    public ExpansionIdMap(File file) throws IOException {
        idMap = new TreeMap<>();
        load(file);
    }

    public void clean() {
        idMap.clear();
    }
    public ExpansionIdMap() {
        idMap = new TreeMap<>();
    }
    
    public void load(File file) throws IOException {
        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            String qid = elems[0];
            String model = elems[1];
            String expansion = elems[2];
            Long id = Long.parseLong(elems[3]);
            put(qid, model, expansion, id);
        }
        reader.close();
    }

    public void output(File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (String qid : idMap.keySet()) {
            for (String model : idMap.get(qid).keySet()) {
                TreeMap<String, Long> map = idMap.get(qid).get(model);
                for (String expansion : map.keySet()) {
                    Long expId = map.get(expansion);
                    writer.write(String.format("%s\t%s\t%s\t%d\n", qid, model, expansion, expId));
                }
            }
        }
        writer.close();
    }

    private void put(String qid, String model, String expansion, Long id) {
        if (!idMap.containsKey(qid)) {
            idMap.put(qid, new TreeMap<String, TreeMap<String, Long>>());
        }

        TreeMap<String, TreeMap<String, Long>> idMap2 = idMap.get(qid);

        if (!idMap2.containsKey(model)) {
            idMap2.put(model, new TreeMap<String, Long>());
        }

        idMap2.get(model).put(expansion, id);
    }

    public boolean contains(String qid, String model, String expansion) {
        return idMap.containsKey(qid) && idMap.get(qid).containsKey(model)
                && idMap.get(qid).get(model).containsKey(expansion);
    }

    public Long getId(String qid, String model, String expansion) {
        if (contains(qid, model, expansion)) {
            return idMap.get(qid).get(model).get(expansion);
        } else {
            return add(qid, model, expansion);
        }
    }

    private Long add(String qid, String model, String expansion) {
        if (!idMap.containsKey(qid)) {
            idMap.put(qid, new TreeMap<String, TreeMap<String, Long>>());
        }

        TreeMap<String, TreeMap<String, Long>> idMap2 = idMap.get(qid);

        if (!idMap2.containsKey(model)) {
            idMap2.put(model, new TreeMap<String, Long>());
        }

        TreeMap<String, Long> map = idMap2.get(model);
        Long id = (long) map.size();
        map.put(expansion, id);
        return id;
    }

}
