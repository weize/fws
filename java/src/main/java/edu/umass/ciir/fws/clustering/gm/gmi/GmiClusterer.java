/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmi;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.gm.GmIndependentClusterer;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiClusterParameters;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiFacetParameters;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.DirectoryUtility;
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
public class GmiClusterer extends StandardStep<TfQueryParameters, TfQueryParameters> {

    String gmPredictDir;
    String gmiClusterDir;
    String gmiRunDir;
    String gmiTuneDir;
    boolean skipExisting;

    public GmiClusterer(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        gmiRunDir = DirectoryUtility.getModelRunDir(p.getString("facetRunDir"), "gmi");
        gmiClusterDir = Utility.getFileName(p.getString("facetTuneDir"), "gmi", "cluster");
        gmPredictDir = DirectoryUtility.getGmPredictDir(gmDir);
        skipExisting = p.get("skipExisting", false);
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        Utility.infoProcessing(queryParams);
        String[] folderIdOptionOthers = Utility.splitParameters(queryParams.text);
        String folderId = folderIdOptionOthers[0];
        String predictOrTune = folderIdOptionOthers[1];
        GmiClusterParameters params = new GmiClusterParameters(queryParams.parameters);
        double termProbTh = params.termProbTh;
        double pairProbTh = params.pairProbTh;

        File termPredictFile;
        File termPairPredictFile;
        File clusterFile;

        if (predictOrTune.equals("predict")) {
            termPredictFile = new File(DirectoryUtility.getGmTermPredictFileName(gmPredictDir, queryParams.id));
            termPairPredictFile = new File(DirectoryUtility.getGmTermPairPredictFileName(gmPredictDir, queryParams.id));
            //folderOptionRankerMetricIndex
            String ranker = folderIdOptionOthers[2];
            String metricIndex = folderIdOptionOthers[3];
            String gmiParams = Utility.parametersToFileNameString(ranker, metricIndex);
            clusterFile = new File(DirectoryUtility.getClusterFilename(gmiClusterDir, queryParams.id, "gmi", gmiParams));

            // move ranker to parameters
            GmiFacetParameters facetParams = new GmiFacetParameters(params.termProbTh, params.pairProbTh, ranker);
            queryParams.parameters = facetParams.toString();
            // do not skip for predicting, should overwrite for new tuning results.
            // if (clusterFile.exists()) { ...
        } else {
            String gmiRunFoldPredictDir = Utility.getFileName(gmiRunDir, folderId, "predict");
            termPredictFile = new File(DirectoryUtility.getGmTermPredictFileName(gmiRunFoldPredictDir, queryParams.id));
            termPairPredictFile = new File(DirectoryUtility.getGmTermPairPredictFileName(gmiRunFoldPredictDir, queryParams.id));
            clusterFile = new File(DirectoryUtility.getGmiFoldClusterFilename(gmiRunDir, folderId, queryParams.id, params.toFilenameString()));
            if (skipExisting && clusterFile.exists()) {
                Utility.infoSkipExisting(clusterFile);
                processor.process(queryParams);
                return;
            }
        }

        Utility.infoOpen(clusterFile);
        Utility.createDirectoryForFile(clusterFile);
        GmIndependentClusterer gmi = new GmIndependentClusterer(termProbTh, pairProbTh);
        List<ScoredFacet> clusters = gmi.cluster(termPredictFile, termPairPredictFile);
        ScoredFacet.output(clusters, clusterFile);
        Utility.infoWritten(clusterFile);
        processor.process(queryParams);
    }
}
