/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmi;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiClusterParameters;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiFacetParameters;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.DirectoryUtility;
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
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class GmiClusterToFacetConverter implements Processor<TfQueryParameters> {

    String gmiClusterDir;
    String gmiFacetDir;
    String gmiRunDir;
    boolean skipExisting;

    public GmiClusterToFacetConverter(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        gmiRunDir = DirectoryUtility.getModelRunDir(p.getString("facetRunDir"), "gmi");
        gmiClusterDir = Utility.getFileName(p.getString("facetTuneDir"), "gmi", "cluster");
        gmiFacetDir = Utility.getFileName(p.getString("facetTuneDir"), "gmi", "facet");
        skipExisting = p.get("skipExisting", false);
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        Utility.infoProcessing(queryParams);

        String[] folderIdOptionOthers = Utility.splitParameters(queryParams.text);
        String folderId = folderIdOptionOthers[0];
        String predictOrTune = folderIdOptionOthers[1];
        GmiFacetParameters params = new GmiFacetParameters(queryParams.parameters);
        String ranker = params.ranker;

        File clusterFile;
        File facetFile;

        if (predictOrTune.equals("predict")) {
            //folderOptionRankerMetricIndex
            //String ranker = folderIdOptionOthers[2];
            String metricIndex = folderIdOptionOthers[3];
            String gmiParams = Utility.parametersToFileNameString(ranker, metricIndex);
            clusterFile = new File(DirectoryUtility.getClusterFilename(gmiClusterDir, queryParams.id, "gmi", gmiParams));
            facetFile = new File(Utility.getFacetFileName(gmiFacetDir, queryParams.id, "gmi", gmiParams));
            // overwrite for new tuning results
            // if (facetFile.exists()) { ...
        } else {
            facetFile = new File(DirectoryUtility.getGmiFoldFacetFilename(gmiRunDir, folderId, queryParams.id, params.toFilenameString()));
            if (skipExisting && facetFile.exists()) {
                Utility.infoSkipExisting(facetFile);
                return;
            } else {
                clusterFile = new File(DirectoryUtility.getGmiFoldClusterFilename(gmiRunDir, folderId, queryParams.id,
                        new GmiClusterParameters(params.termProbTh, params.pairProbTh).toFilenameString()));

            }
        }

        Utility.infoOpen(facetFile);
        List<ScoredFacet> clusters = ScoredFacet.loadClusters(clusterFile);
        Utility.createDirectoryForFile(facetFile);
        if (ranker.equals("avg")) {
            ScoredFacet.avgScoreAndRank(clusters);
        }
        ScoredFacet.outputAsFacets(clusters, facetFile);
        Utility.infoWritten(facetFile);
    }

    @Override
    public void close() throws IOException {
    }
}
