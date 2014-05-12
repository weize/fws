/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

/**
 *
 * @author wkong
 */
public class CandidateListDocFreqMap {

    // fileds: terms + 
    public final static int _tf = 0;
    public final static int _df = 1;
    public final static int _ulTf = 2;
    public final static int _ulDf = 3;
    public final static int _olTf = 4;
    public final static int _olDf = 5;
    public final static int _slTf = 6;
    public final static int _slDf = 7;
    public final static int _trTf = 8;
    public final static int _trDf = 9;
    public final static int _tdTf = 10;
    public final static int _tdDf = 11;
    public final static int _txTf = 12;
    public final static int _txDf = 13;
    public final static int size = 14;

    HashMap<String, long[]> clistDfs;
    long[] clistCdfs;

    public CandidateListDocFreqMap(File clistDfFile, File clistDfMetaFile) throws IOException {
        clistCdfs = new long[size];
        clistDfs = new HashMap<>();
        loadMeta(clistDfMetaFile);
        loadCandidateListDocFreqs(clistDfFile);
    }

    CandidateListDocFreqMap(File clistDfFile, File clistDfMetaFile, TreeMap<String, TermFeaturesExtractor.TermFeatures> terms) throws IOException {
        clistCdfs = new long[size];
        clistDfs = new HashMap<>();
        loadMeta(clistDfMetaFile);
        loadCandidateListDocFreqs(clistDfFile, terms);
    }

    private void loadCandidateListDocFreqs(File file) throws IOException {

        clistDfs.clear();

        BufferedReader reader = Utility.getReader(file);
        String line = reader.readLine();
        String[] elems = line.split("\t");
        assert elems.length == size + 1;

        do {
            elems = line.split("\t");
            String term = elems[0];
            long[] curDFs = new long[size];
            for (int i = 1; i < elems.length; i++) {
                curDFs[i - 1] = Integer.parseInt(elems[i]);
            }
            clistDfs.put(term, curDFs);
        } while ((line = reader.readLine()) != null);

        reader.close();
    }

    
    /**
     * Due to memory issue, only load dfs in the term set.
     * @param file
     * @param terms
     * @throws IOException 
     */
    private void loadCandidateListDocFreqs(File file, TreeMap<String, TermFeaturesExtractor.TermFeatures> terms) throws IOException {
        clistDfs.clear();

        BufferedReader reader = Utility.getReader(file);
        String line = reader.readLine();
        String[] elems = line.split("\t");
        assert elems.length == size + 1;

        do {
            elems = line.split("\t");
            String term = elems[0];
            if (terms.containsKey(term)) {
                long[] curDFs = new long[size];
                for (int i = 1; i < elems.length; i++) {
                    curDFs[i - 1] = Integer.parseInt(elems[i]);
                }
                clistDfs.put(term, curDFs);
            }
        } while ((line = reader.readLine()) != null);

        reader.close();
    }

    // assume the frequency is 1 if the term is not found in the candidate list document frequency file.
    public long getDf(String term, int index) {
        return clistDfs.containsKey(term) ? clistDfs.get(term)[index] : 1;
    }

    double getCdf(int index) {
        return clistCdfs[index];
    }

    private void loadMeta(File file) throws IOException {
        BufferedReader reader = Utility.getReader(file);
        String line;
        reader.readLine(); // read header
        line = reader.readLine(); // read col freqs
        String[] elems = line.split("\t");
        assert elems.length == size + 1;

        for (int i = 1; i < elems.length; i++) {
            clistCdfs[i - 1] = Integer.parseInt(elems[i]);
        }
        reader.close();
    }

}
