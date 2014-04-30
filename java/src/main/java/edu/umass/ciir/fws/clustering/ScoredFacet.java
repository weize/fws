/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author wkong
 */
public class ScoredFacet implements Comparable<ScoredFacet> {

    public double score;
    public List<ScoredItem> items;

    public ScoredFacet(List<ScoredItem> items, double score) {
        this.score = score;
        this.items = items;
    }

    public ScoredFacet() {

    }

    public static void output(List<ScoredFacet> facets, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (ScoredFacet f : facets) {
            writer.write(f.toString() + "\n");
        }
        writer.close();
    }

    public static void outputAsFacets(List<ScoredFacet> facets, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (ScoredFacet f : facets) {
            writer.write(f.score + "\t");
            List<ScoredItem> items = f.items;
            if (items.size() > 0) {
                writer.write(items.get(0).item);
            }

            for (int i = 1; i < items.size(); i++) {
                writer.write("|" + items.get(i).item);
            }
            writer.newLine();
        }
        writer.close();
    }

    public static List<ScoredFacet> load(File file) throws IOException {
        ArrayList<ScoredFacet> facets = new ArrayList<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            double score = Double.parseDouble(fields[0]);
            String scoredItemList = fields[1];

            ArrayList<ScoredItem> items = new ArrayList<>();
            for (String scoredItemStr : scoredItemList.split("\\|")) {
                ScoredItem scoredItem = new ScoredItem(scoredItemStr);
                items.add(scoredItem);
            }

            facets.add(new ScoredFacet(items, score));
        }
        reader.close();
        return facets;
    }

    @Override
    public String toString() {
        return score + "\t" + TextProcessing.join(items, "|");
    }

    @Override
    public int compareTo(ScoredFacet other) {
        return Utility.compare(other.score, this.score);
    }

}
