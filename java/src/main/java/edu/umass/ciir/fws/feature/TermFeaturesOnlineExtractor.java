/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.demo.search.GalagoSearchEngine;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.utility.Utility;
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
    boolean extractDf;

    public TermFeaturesOnlineExtractor(GalagoSearchEngine galago, CandidateListDocFreqMap clistDfMap, Parameters p) {
        this.galago = galago;
        this.termFeatures = new TreeMap();
        clueCdf = p.getLong("clueCdf");
        extractDf = p.getBoolean("clueDocFreq");
        this.clistDfs = clistDfMap;

    }

    public TreeMap<String, TermFeatures> extract(List<CandidateList> clists, List<RankedDocument> docs) {
        this.clists = clists;
        this.docs = docs;

        initializeTermFeatures();
        Utility.info("#candidate items = " + termFeatures.size());

        Utility.info("extracting term length");
        extractTermLength();
        // features based on documents
        Utility.info("extracting doc features in content");
        extractDocFeaturesInContentField();
        Utility.info("extracting doc features in title");
        extractDocFeaturesInTitleField();

        // extrac list features based on different set of candidates lists
        Utility.info("extracting list features");
        for (String type : CandidateList.clistTypes) {
            extractListFeatures(type);
        }

        Utility.info("extracting document frequency ...");
        extractClueWebIDF();
        extractCandidateListIDF();
        return termFeatures;
    }

    @Override
    protected double getClueDocFreq(String term) {
        if (!extractDf) return clueCdf / 10000;
        long df = galago.getDocFreq(term);
        return df == -1 ? 0 : df;
    }

    @Override
    protected double getCollectionDocFreq() {
        return clueCdf;
    }
}
