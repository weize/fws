/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.demo.search.GalagoSearchEngine;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class TermFeaturesOnlineExtractor extends TermFeaturesExtractor {

    GalagoSearchEngine galago;
    double clueCdf;

    public TermFeaturesOnlineExtractor(GalagoSearchEngine galago, Parameters p) {
        this.galago = galago;
        this.termFeatures = new TreeMap();
        clistDfFile = p.getString("clistDfFile");
        clistDfMetaFile = p.getString("clistDfMetaFile");
        clueCdf = p.getLong("clueCdf");

    }

    public List<TermFeatures> extract(List<CandidateList> clists, List<RankedDocument> docs) {
        this.clists = clists;
        this.docs = docs;

        initializeTermFeatures();
        try {
            clistDfs = new CandidateListDocFreqMap(new File(clistDfFile), new File(clistDfMetaFile), termFeatures);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        extractTermLength();
        // features based on documents
        extractDocFeaturesInContentField();
        extractDocFeaturesInTitleField();

        // extrac list features based on different set of candidates lists
        for (String type : CandidateList.clistTypes) {
            extractListFeatures(type);
        }

        extractClueWebIDF();
        extractCandidateListIDF();
        for (TermFeatures f : termFeatures.values()) {
            System.out.println(f.toString());
        }
        return null;
    }

    @Override
    protected double getClueDocFreq(String term) {
        long df = galago.getDocFreq(term);
        System.err.println(term + ":" + df);
        return df == -1 ? 0 : df;
    }

    @Override
    protected double getCollectionDocFreq() {
        return clueCdf;
    }
}
