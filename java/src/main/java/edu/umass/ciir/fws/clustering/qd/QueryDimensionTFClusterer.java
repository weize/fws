/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clustering.qd.QdParameterSettings.QdClusterParameters;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.DirectoryUtility;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
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
public class QueryDimensionTFClusterer extends QueryDimensionClusterer implements Processor<TfQueryParameters> {

    String clusterDir;
    String featureDir;

    public QueryDimensionTFClusterer(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        debug = p.get("debug", false);
        String facetRunDir = p.getString("facetRunDir");
        featureDir = DirectoryUtility.getFeatureDir(facetRunDir, modelName);
        clusterDir = DirectoryUtility.getCluterDir(facetRunDir, modelName);
    }

    @Override
    public void process(TfQueryParameters queryParameters) throws IOException {
        System.err.println(String.format("Processing qid:%s parameters:%s", queryParameters.id, queryParameters.parameters));

        //setQueryParameters
        String qid = queryParameters.id;
        QdClusterParameters params = new QdClusterParameters(queryParameters.parameters);
        distanceMax = params.distanceMax;
        websiteCountMin = params.websiteCountMin;

        File clusterFile = new File(DirectoryUtility.getClusterFilename(clusterDir, qid, modelName, params.toFilenameString()));

//      existing files should be checked in the parameter genearting stage
//        if (clusterFile.exists()) {
//            Utility.infoFileExists(clusterFile);
//            return;
//        }
        List<FacetFeatures> nodes = loadFacetFeatures(qid);
        List<QDCluster> clusters = cluster(nodes, distanceMax, websiteCountMin);

        Utility.createDirectoryForFile(clusterFile);
        output(clusters, clusterFile);
    }

    private List<FacetFeatures> loadFacetFeatures(String qid) throws IOException {
        String facetFeaturesFile = Utility.getQdFacetFeatureFileName(featureDir, qid);
        return FacetFeatures.readFromFile(facetFeaturesFile);
    }

    private void output(List<QDCluster> clusters, File file) throws IOException {
        Utility.infoOpen(file);
        Writer writer = Utility.getWriter(file);
        for (QDCluster c : clusters) {
            if (c.items.size() < 2) {
                continue;
            }
            writer.write(String.format("%s\t%s\t%s\n", c.score, c.numOfSite, TextProcessing.join(c.items, "|")));
        }
        writer.close();
        Utility.infoWritten(file);
    }

    @Override
    public void close() throws IOException {

    }
}
