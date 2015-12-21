/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.types.TfQueryParameters;
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
        String runDir = p.getString("qdRunDir");
        featureDir = Utility.getFileName(runDir, "feature");
        clusterDir = Utility.getFileName(runDir, "cluster");
    }

    @Override
    public void process(TfQueryParameters queryParameters) throws IOException {
        System.err.println(String.format("Processing qid:%s parameters:%s", queryParameters.id, queryParameters.parameters));

        //setQueryParameters
        String qid = queryParameters.id;
        String[] fields = Utility.splitParameters(queryParameters.parameters);
        distanceMax = Double.parseDouble(fields[0]);
        websiteCountMin = Double.parseDouble(fields[1]);

        File clusterFile = new File(Utility.getQdClusterFileName(clusterDir, qid, distanceMax, websiteCountMin));
        if (clusterFile.exists()) {
            Utility.infoFileExists(clusterFile);
            return;
        }
        
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
