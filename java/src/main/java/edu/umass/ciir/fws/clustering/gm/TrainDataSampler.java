/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author wkong
 */
public class TrainDataSampler {

    int negPosRatio = 2; // 
    List<String> pos; // postive data
    List<String> neg; // negative data

    public TrainDataSampler() {
        this.pos = new ArrayList<>();
        this.neg = new ArrayList<>();
    }

    public static class DataString {

        int rating;
        String dataStr;

        public DataString(int rating, String dataStr) {
            this.rating = rating;
            this.dataStr = dataStr;
        }
    }

    public void sampleToFile(List<File> dataFiles, File outfile) throws IOException {
        load(dataFiles);
        sample();
        output(outfile);

    }

    public static void combine(List<File> dataFiles, File outfile) throws IOException {
        BufferedWriter writer = Utility.getWriter(outfile);
        for (File file : dataFiles) {
            BufferedReader reader = Utility.getReader(file);
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            reader.close();
        }
        writer.close();
    }

    private void output(File outfile) throws IOException {
        BufferedWriter writer = Utility.getWriter(outfile);
        List<String> selected = new ArrayList<String>();
        selected.addAll(pos);
        selected.addAll(neg);
        Collections.shuffle(selected);
        for (String data : selected) {
            writer.write(data);
            writer.newLine();
        }

        writer.close();
    }

    private void sample() {
        int posSize = pos.size();
        int negSize = Math.min(neg.size(), posSize * negPosRatio);

        // random select negSize neg data 
        Collections.shuffle(neg);
        neg = neg.subList(0, negSize);
    }

    private void load(List<File> dataFiles) throws IOException {
        pos.clear();
        neg.clear();

        for (File file : dataFiles) {
            BufferedReader reader = Utility.getReader(file);
            String line;
            while ((line = reader.readLine()) != null) {
                int rating = Integer.parseInt(line.substring(0, line.indexOf('\t')));
                if (rating < 0) {
                    neg.add(line);
                } else {
                    pos.add(line);
                }
            }
            reader.close();
        }
    }

}
