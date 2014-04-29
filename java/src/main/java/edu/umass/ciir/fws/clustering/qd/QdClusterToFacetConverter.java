/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.types.QueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
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
@InputClass(className = "edu.umass.ciir.fws.types.QueryParameters")
public class QdClusterToFacetConverter implements Processor<QueryParameters> {

    String facetDir;
    String clusterDir;

    public QdClusterToFacetConverter(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        facetDir = p.getString("qdFacetDir");
        clusterDir = p.getString("qdClusterDir");
    }

    @Override
    public void process(QueryParameters queryParameters) throws IOException {
        System.err.println(String.format("Processing qid:%s parameters:%s", queryParameters.id, queryParameters.parameters));
        String qid = queryParameters.id;
        String[] fields = Utility.splitParameters(queryParameters.parameters);
        double distanceMax = Double.parseDouble(fields[0]);
        double websiteCountMin = Double.parseDouble(fields[1]);
        double itemRatio = Double.parseDouble(fields[2]);

        String clusterFileName = Utility.getQdClusterFileName(clusterDir, qid, distanceMax, websiteCountMin);
        BufferedReader reader = Utility.getReader(clusterFileName);
        String facetFileName = Utility.getQdFacetFileName(clusterDir, qid, distanceMax, websiteCountMin, itemRatio);
        BufferedWriter writer = Utility.getWriter(facetFileName);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] fields2 = line.split("\t");
            double score = Double.parseDouble(fields2[0]);
            int siteNum = Integer.parseInt(fields2[1]);
            double threshold = siteNum * itemRatio;
            String facetTermList = fields2[2];

            ArrayList<String> items = new ArrayList<>();
            for (String scoredItemStr : facetTermList.split("\\|")) {
                ScoredItem scoredItem = new ScoredItem(scoredItemStr);
                if (scoredItem.score > threshold) {
                    items.add(scoredItem.item);
                }
            }

            if (items.size() > 0) {
                writer.write(score + "\t" + TextProcessing.join(items, "|")+"\n");
            }

        }
        reader.close();
        writer.close();
        System.err.println("Written in " + facetFileName);
    }

    @Override
    public void close() throws IOException {

    }

}
