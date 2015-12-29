/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws.clustering.lda;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * Use first terms in the cluster as facet terms.
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class LdaClusterToFacetConverter implements Processor<TfQueryParameters> {
    String facetDir;
    String clusterDir;

    public LdaClusterToFacetConverter(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String runDir = p.getString("ldaRunDir");
        facetDir = Utility.getFileName(runDir, "facet");
        clusterDir = Utility.getFileName(runDir, "cluster");
    }

    @Override
    public void process(TfQueryParameters queryParameters) throws IOException {
        System.err.println(String.format("Processing qid:%s parameters:%s", queryParameters.id, queryParameters.parameters));
        String qid = queryParameters.id;
        LdaParameterSettings.LdaFacetParameters params = new LdaParameterSettings.LdaFacetParameters(queryParameters.parameters);
        long topicNum = params.topicNum;
        long termNum = params.termNum;
        File facetFile = new File(Utility.getLdaFacetFileName(facetDir, qid, params.toFilenameString()));
        if (facetFile.exists()) {
            Utility.infoFileExists(facetFile);
            return;
        }
        // loadClusters clusters
        File clusterFile = new File(Utility.getLdaClusterFileName(clusterDir, qid, topicNum));
        List<ScoredFacet> clusters = ScoredFacet.loadClusters(clusterFile);
        // select facet terms
        for (ScoredFacet cluster : clusters) {
            int size = (int) Math.min(termNum, cluster.items.size());
            cluster.items = cluster.items.subList(0, size);
        }
        Utility.infoOpen(facetFile);
        Utility.createDirectoryForFile(facetFile);
        ScoredFacet.outputAsFacets(clusters, facetFile);
        Utility.infoWritten(facetFile);
    }

    @Override
    public void close() throws IOException {
    }
    
}
