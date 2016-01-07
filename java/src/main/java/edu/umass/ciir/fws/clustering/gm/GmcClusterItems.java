/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.ScoredFacet;
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
public class GmcClusterItems extends StandardStep<TfQueryParameters, TfQueryParameters> {

    String clusterDir;
    String predictDir;

    public GmcClusterItems(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        clusterDir = p.getString("gmcClusterDir");
        String gmDir = Utility.getFileName(p.getString("facetRunDir"), "gm");
        predictDir = Utility.getFileName(gmDir, "predict");
        
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        File termPredictFile = new File(DirectoryUtility.getGmTermPredictFileName(predictDir, queryParams.id));
        File termPairPredictFile = new File(DirectoryUtility.getGmTermPairPredictFileName(predictDir, queryParams.id));

        Utility.infoProcessing(queryParams);

        File clusterFile = new File(Utility.getGmcClusterFileName(clusterDir, queryParams.id));
        Utility.createDirectoryForFile(clusterFile);

        GmCoordinateAscentClusterer gmc = new GmCoordinateAscentClusterer();
        List<ScoredFacet> clusters = gmc.cluster(termPredictFile, termPairPredictFile);
        ScoredFacet.output(clusters, clusterFile);
        Utility.infoWritten(clusterFile);
        processor.process(queryParams);
    }
}
