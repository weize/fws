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
    
    public ScoredFacet(double score) {
        this.score = score;
        items = new ArrayList<>();
    }
    
    public void addItem(ScoredItem item) {
        items.add(item);
    }
    
    public int size() {
        return items.size();
    }

    public static void output(List<?> facets, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (Object f : facets) {
            writer.write(((ScoredFacet) f).toString() + "\n");
        }
        writer.close();
    }

    public static void outputAsFacets(List<ScoredFacet> facets, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (ScoredFacet f : facets) {
            writer.write(f.toFacetString());
            writer.newLine();
        }
        writer.close();
    }

    public String getItemList() {
        StringBuilder itemList = new StringBuilder();
        if (items.size() > 0) {
            itemList.append(items.get(0).item);
        }

        for (int i = 1; i < items.size(); i++) {
            itemList.append("|").append(items.get(i).item);
        }
        return itemList.toString();
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

    public String toFacetString() {
        return score + "\t" + getItemList();
    }

    @Override
    public int compareTo(ScoredFacet other) {
        return Utility.compare(other.score, this.score);
    }

}
