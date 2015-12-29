/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Terms are selected if it pass the condition (score > NumSite *
 * itemRatio). See paper "Finding dimensions for queries".
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class QdClusterToFacetConverter implements Processor<TfQueryParameters> {
    String facetDir;
    String clusterDir;
    String runDir;

    public QdClusterToFacetConverter(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        runDir = p.getString("qdRunDir");
        facetDir = Utility.getFileName(runDir, "facet");
        clusterDir = Utility.getFileName(runDir, "cluster");
    }

    @Override
    public void process(TfQueryParameters queryParameters) throws IOException {
        System.err.println(String.format("Processing qid:%s parameters:%s", queryParameters.id, queryParameters.parameters));
        String qid = queryParameters.id;
        QdParameterSettings.QdFacetParameters params = new QdParameterSettings.QdFacetParameters(queryParameters.parameters);
        double distanceMax = params.distanceMax;
        double websiteCountMin = params.websiteCountMin;
        double itemRatio = params.itemRatio;
        double itemThreshold = params.itemThreshld;
        File facetFile = new File(Utility.getQdFacetFileName(facetDir, qid, params.toFilenameString()));
        // should not exists because we filtered them out in generating params process
        if (facetFile.exists()) {
            Utility.infoFileExists(facetFile);
            return;
        }
        // loadClusters clusters
        String clusterFileName = Utility.getQdClusterFileName(clusterDir, qid, distanceMax, websiteCountMin);
        BufferedReader reader = Utility.getReader(clusterFileName);
        String line;
        ArrayList<ScoredFacet> facets = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String[] fields2 = line.split("\t");
            double score = Double.parseDouble(fields2[0]);
            int siteNum = Integer.parseInt(fields2[1]);
            double threshold = siteNum * itemRatio;
            String facetTermList = fields2[2];
            ArrayList<ScoredItem> items = new ArrayList<>();
            for (String scoredItemStr : facetTermList.split("\\|")) {
                ScoredItem scoredItem = new ScoredItem(scoredItemStr);
                if (scoredItem.score > itemThreshold && scoredItem.score > threshold) {
                    items.add(scoredItem);
                }
            }
            if (items.size() > 0) {
                facets.add(new ScoredFacet(items, score));
            }
        }
        Utility.infoOpen(facetFile);
        Utility.createDirectoryForFile(facetFile);
        ScoredFacet.outputAsFacets(facets, facetFile);
        Utility.infoWritten(facetFile);
    }

    @Override
    public void close() throws IOException {
    }
    
}
