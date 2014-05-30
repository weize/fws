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
public class GmiClusterItems extends StandardStep<TfQueryParameters, TfQueryParameters> {

    String predictDir;
    String gmiClusterDir;
    String trainDir;

    public GmiClusterItems(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        predictDir = Utility.getFileName(gmDir, "predict");
        gmiClusterDir = p.getString("gmiClusterDir");
        trainDir = Utility.getFileName(gmDir, "train");
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        String[] params = Utility.splitParameters(queryParams.parameters);
        String folderId = params[0];
        String predictOrTune = params[1];
        double termProbTh = Double.parseDouble(params[2]);
        double pairProbTh = Double.parseDouble(params[3]);

        File termPredictFile;
        File termPairPredictFile;
        File clusterFile;

        if (predictOrTune.equals("predict")) {
            termPredictFile = new File(Utility.getGmTermPredictFileName(predictDir, queryParams.id));
            termPairPredictFile = new File(Utility.getGmTermPairPredictFileName(predictDir, queryParams.id));
            //param 4 is the ranker "avg" or "sum", parameter file is metric index.
            String gmiParam = Utility.parametersToFileNameString(params[4], params[5]);
            clusterFile = new File(Utility.getClusterFileName(gmiClusterDir, queryParams.id, "gmi", gmiParam));

        } else {
            String tuneDir = Utility.getFileName(trainDir, folderId, "tune");
            termPredictFile = new File(Utility.getGmTermPredictFileName(tuneDir, queryParams.id));
            termPairPredictFile = new File(Utility.getGmTermPairPredictFileName(tuneDir, queryParams.id));
            String gmiParam = Utility.parametersToFileNameString(termProbTh, pairProbTh);
            clusterFile = new File(Utility.getClusterFileName(tuneDir, queryParams.id, "gmi", gmiParam));
        }

        Utility.infoProcessing(queryParams);
        Utility.createDirectoryForFile(clusterFile);
        GmIndependentClusterer gmi = new GmIndependentClusterer(termProbTh, pairProbTh);
        List<ScoredFacet> clusters = gmi.cluster(termPredictFile, termPairPredictFile);
        ScoredFacet.output(clusters, clusterFile);
        Utility.infoWritten(clusterFile);
        processor.process(queryParams);
    }
}
