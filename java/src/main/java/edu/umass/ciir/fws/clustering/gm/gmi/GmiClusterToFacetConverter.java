/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmi;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiFacetParameters;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
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
public class GmiClusterToFacetConverter extends StandardStep<TfQueryParameters, TfQueryParameters> {

    String gmiClusterDir;
    String gmiFacetDir;
    String trainDir;

    public GmiClusterToFacetConverter(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        gmiClusterDir = p.getString("gmiClusterDir");
        gmiFacetDir = p.getString("gmiFacetDir");
        trainDir = Utility.getFileName(gmDir, "train");
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        Utility.infoProcessing(queryParams);

        String[] folderIdOptionOthers = Utility.splitParameters(queryParams.text);
        String folderId = folderIdOptionOthers[0];
        String predictOrTune = folderIdOptionOthers[1];
        GmiFacetParameters params = new GmiFacetParameters(queryParams.parameters);
        double termProbTh = params.termProbTh;
        double pairProbTh = params.pairProbTh;
        String ranker = params.ranker;

        String tuneDir = Utility.getFileName(trainDir, folderId, "tune");

        File clusterFile;
        File facetFile;

        if (predictOrTune.equals("predict")) {            
            //folderOptionRankerMetricIndex
            //String ranker = folderIdOptionOthers[2];
            String metricIndex = folderIdOptionOthers[3];
            String gmiParams = Utility.parametersToFileNameString(ranker, metricIndex);
            clusterFile = new File(Utility.getClusterFileName(gmiClusterDir, queryParams.id, "gmi", gmiParams));
            facetFile = new File(Utility.getFacetFileName(gmiFacetDir, queryParams.id, "gmi", gmiParams));
            // overwrite for new tuning results
            // if (facetFile.exists()) { ...
        } else {
            clusterFile = new File(Utility.getGmiClusterFileName(tuneDir, queryParams.id, termProbTh, pairProbTh));
            facetFile = new File(Utility.getFacetFileName(tuneDir, queryParams.id, "gmi", params.toFilenameString()));

            // skip for tunning cases because
            // if the file name match, the results are always the same
            if (facetFile.exists()) {
                Utility.infoFileExists(facetFile);
                processor.process(queryParams);
                return;
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
        processor.process(queryParams);
    }
}
