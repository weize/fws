/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmj;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.gm.GmJointClusterer;
import static edu.umass.ciir.fws.clustering.lda.LdaClusterer.modelName;
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
public class GmjClusterer implements Processor<TfQueryParameters> {

    public final static String modelName = "gmj";
    String clusterDir;
    String predictDir;

    public GmjClusterer(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String facetRunDir = p.getString("facetRunDir");
        clusterDir = DirectoryUtility.getCluterDir(facetRunDir, modelName);
        String gmDir = p.getString("gmDir");
        predictDir = DirectoryUtility.getGmPredictDir(gmDir);
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        File termPredictFile = new File(DirectoryUtility.getGmTermPredictFileName(predictDir, queryParams.id));
        File termPairPredictFile = new File(DirectoryUtility.getGmTermPairPredictFileName(predictDir, queryParams.id));

        Utility.infoProcessing(queryParams);

        File clusterFile = new File(DirectoryUtility.getClusterFilename(clusterDir, queryParams.id, modelName, ""));
        Utility.createDirectoryForFile(clusterFile);

        GmJointClusterer gmj = new GmJointClusterer();
        List<ScoredFacet> clusters = gmj.cluster(termPredictFile, termPairPredictFile);
        ScoredFacet.output(clusters, clusterFile);
        Utility.infoWritten(clusterFile);
    }

    @Override
    public void close() throws IOException {
    }
}
