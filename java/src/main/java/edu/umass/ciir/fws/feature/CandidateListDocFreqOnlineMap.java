/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import static edu.umass.ciir.fws.feature.CandidateListDocFreqMap.size;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import org.lemurproject.galago.core.btree.simple.DiskMapReader;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class CandidateListDocFreqOnlineMap extends CandidateListDocFreqMap {

    DiskMapReader clistDfMap;

    public CandidateListDocFreqOnlineMap(Parameters p) {
        try {
            String clistDfIndex = p.getString("clistDfIndex");
            String clistDfMetaFile = p.getString("clistDfMetaFile");
            clistCdfs = new long[size];
            loadMeta(new File(clistDfMetaFile));
            loadCandidateListDocFreqs(new File(clistDfIndex));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void loadCandidateListDocFreqs(File file) {
        Utility.info("loading clistDf index");
        try {
            clistDfMap = new DiskMapReader(file.getAbsolutePath());

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public long getDf(String term, int index) {
        byte[] dfs = clistDfMap.get(term);
        if (dfs == null) {
            return 1;
        }

        return ((long[]) Utility.convertFromBytes(dfs))[index];
    }

}
