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
public class GmiClusterToFacetConverter extends StandardStep<TfQueryParameters, TfQueryParameters> {

    String predictDir;
    String trainDir;

    public GmiClusterToFacetConverter(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        predictDir = Utility.getFileName(gmDir, "predict");
        trainDir = Utility.getFileName(gmDir, "train");
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        Utility.infoProcessingQuery(queryParams.id);

        String[] params = Utility.splitParameters(queryParams.parameters);
        String folderId = params[0];
        String predictOrTune = params[1];
        double termProbTh = Double.parseDouble(params[2]);
        double pairProbTh = Double.parseDouble(params[3]);
        String ranker = params[4];

        String tuneDir = Utility.getFileName(trainDir, folderId, "tune");
        String workingDir = predictOrTune.equals("predict") ? predictDir : tuneDir;

        // loadClusters clusters
        File clusterFile = new File(Utility.getGmiClusterFileName(workingDir, queryParams.id, termProbTh, pairProbTh));
        List<ScoredFacet> clusters = ScoredFacet.loadClusters(clusterFile);

        String gmiParam = Utility.parametersToFileNameString(termProbTh, pairProbTh, ranker);
        File facetFile = new File(Utility.getFacetFileName(workingDir, queryParams.id, "gmi", gmiParam));
        Utility.createDirectoryForFile(facetFile);
        if (ranker.equals("avg")) {
            ScoredFacet.avgScoreAndRank(clusters);
        }
        ScoredFacet.outputAsFacets(clusters, facetFile);
        Utility.infoWritten(facetFile);
        processor.process(queryParams);
    }
}
