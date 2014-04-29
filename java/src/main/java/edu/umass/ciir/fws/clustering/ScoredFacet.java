/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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

    @Override
    public String toString() {
        return score + "\t" + TextProcessing.join(items, "|");
    }

    @Override
    public int compareTo(ScoredFacet other) {
        return Utility.compare(other.score, this.score);
    }

}
