/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.study.features;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class GenerateConfigForAccumulateFeatures extends AppFunction {

    @Override
    public String getName() {
        return "gen-config-acc-features";
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run(Parameters param, PrintStream output) throws Exception {
        String outdir = param.getAsString("outdir");
        param.remove("outdir");
        ArrayList<Integer> tIndice = new ArrayList<>();
        ArrayList<Integer> pIndice = new ArrayList<>();
        
        // feature index; negative indice for pair features, e.g., -3, 3 for pair feautre
        int [] order = new int[] {33, -3, 13, 29, 22, 12, 14, 18, 32, -4, 28, 16, 31, 19, 30, 21, 27, 26, 6, 11, 17, 1, 23, 7, 5, 10, 25, 8, 9, -2, 24, 15, 20, -1, };
        
        int fileId = 0;
        for (int idx : order) {
            if (idx < 0) {
                pIndice.add(-idx);
            } else {
                tIndice.add(idx);
            }
            
            if (!pIndice.isEmpty() && !pIndice.isEmpty()) {
                Parameters p = param.clone();
                p.set("termFeatureIndices", tIndice);
                p.set("pairFeatureIndices", pIndice);
                fileId ++;
                String facetRunDir = param.getString("facetRunDir");
                p.set("facetRunDir", facetRunDir + "-" + fileId);
                String facetTuneDir = param.getString("facetTuneDir");
                p.set("facetTuneDir", facetTuneDir + "-" + fileId);
                output(p, String.format("%s/config-accu-feature-%d.json", outdir, fileId));
            }
        }
        
    }

    private void output(Parameters p, String filename) throws IOException {
        BufferedWriter writer = Utility.getWriter(filename);
        writer.write(p.toPrettyString());
        writer.close();
    }
    
}
