/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Candidate list document frequency in a gloabl candidate list set. There are 6
 * types of "df" here.
 *
 * @author wkong
 */
public class CandidateListDocFreqExtractor {

    HashMap<String, long[]> clistDfs;
    long[] clistCdfs; // number of "document" in the gloabl candidate list set.
    String clistDfFile;
    
    public CandidateListDocFreqExtractor(String CandidateListDocFreqFileName) throws IOException {
        clistDfFile = CandidateListDocFreqFileName;
        clistDfs = new HashMap<>();
        clistCdfs = new long[6];
        loadCandidateListDocFreqs();
    }

    private void loadCandidateListDocFreqs() throws IOException {
        clistDfs.clear();
        
        BufferedReader reader = Utility.getReader(clistDfFile);
        String line;
        reader.readLine(); // read header
        line = reader.readLine(); // read col freqs
        String[] elems = line.split("\t");
        for (int i = 1; i < elems.length; i++) {
            this.clistCdfs[i - 1] = Integer.parseInt(elems[i]);
        }

        while ((line = reader.readLine()) != null) {
            String[] elems2 = line.split("\t");
            String term = elems2[0];
            long[] curDFs = new long[6];
            for (int i = 1; i < elems2.length; i++) {
                curDFs[i - 1] = Integer.parseInt(elems2[i]);
            }
            clistDfs.put(term, curDFs);
        }
        reader.close();
    }
    
}
