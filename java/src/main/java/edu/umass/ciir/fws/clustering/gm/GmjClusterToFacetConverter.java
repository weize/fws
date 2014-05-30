/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class GmjClusterToFacetConverter extends StandardStep<TfQueryParameters, TfQueryParameters> {

    String facetDir;
    String clusterDir;

    public GmjClusterToFacetConverter(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        facetDir = p.getString("gmjFacetDir");
        clusterDir = p.getString("gmjClusterDir");
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        Utility.infoProcessing(queryParams);
        String [] params = Utility.splitParameters(queryParams.parameters);
        String ranker = params[params.length-1]; // last one should be ranker
        
        // loadClusters clusters
        File clusterFile = new File(Utility.getGmjClusterFileName(clusterDir, queryParams.id));
        List<ScoredFacet> clusters = ScoredFacet.loadClusters(clusterFile);

        File facetFile = new File(Utility.getFacetFileName(facetDir, queryParams.id, "gmj", ranker));
        Utility.createDirectoryForFile(facetFile);
        if (ranker.equals("avg")) {
            ScoredFacet.avgScoreAndRank(clusters);
        }
        ScoredFacet.outputAsFacets(clusters, facetFile);
        Utility.infoWritten(facetFile);
        processor.process(queryParams);
    }
}
