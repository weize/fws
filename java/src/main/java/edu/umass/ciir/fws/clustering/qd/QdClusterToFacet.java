package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.types.QueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Tupleflow application that does reads in query dimension clusters and output
 * query facets.
 *
 *
 * @author wkong
 */
public class QdClusterToFacet extends ProcessQueryParametersApp {

    @Override
    protected Class getProcessClass() {
        return QdClusterToFacetConverter.class;
    }

    @Override
    public String getName() {
        return "facet-qd";
    }

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateQdFacetParameters.class;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.Query")
    @OutputClass(className = "edu.umass.ciir.fws.types.QueryParameters")
    public static class GenerateQdFacetParameters extends StandardStep<Query, QueryParameters> {

        List<Double> distanceMaxs;
        List<Double> websiteCountMins;
        List<Double> itemRatios;

        public GenerateQdFacetParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            distanceMaxs = p.getList("qdDistanceMaxs");
            websiteCountMins = p.getList("qdWebsiteCountMins");
            itemRatios = p.getList("qdItemRatios");

        }

        @Override
        public void process(Query query) throws IOException {
            for (double distanceMax : distanceMaxs) {
                for (double websiteCountMin : websiteCountMins) {
                    for (double itemRatio : itemRatios) {
                        String parameters = Utility.parametersToString(distanceMax, websiteCountMin, itemRatio);
                        processor.process(new QueryParameters(query.id, query.text, parameters));
                    }
                }
            }

        }

    }

    /**
     * Terms are selected if it pass the condition (score > NumSite *
     * itemRatio). See paper "Finding dimensions for queries".
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.QueryParameters")
    public static class QdClusterToFacetConverter implements Processor<QueryParameters> {

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

            // load clusters
            String clusterFileName = Utility.getQdClusterFileName(clusterDir, qid, distanceMax, websiteCountMin);
            BufferedReader reader = Utility.getReader(clusterFileName);

            String line;
            ArrayList<ScoredFacet> facets = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                String[] fields2 = line.split("\t");
                double score = Double.parseDouble(fields2[0]);
                int siteNum = Integer.parseInt(fields2[1]);
                double threshold = siteNum * itemRatio;
                String facetTermList = fields2[2];

                ArrayList<ScoredItem> items = new ArrayList<>();
                for (String scoredItemStr : facetTermList.split("\\|")) {
                    ScoredItem scoredItem = new ScoredItem(scoredItemStr);
                    if (scoredItem.score > 1 && scoredItem.score > threshold) {
                        items.add(scoredItem);
                    }
                }

                if (items.size() > 0) {
                    facets.add(new ScoredFacet(items, score));
                }

            }
            File facetFile = new File(Utility.getQdFacetFileName(facetDir, qid, distanceMax, websiteCountMin, itemRatio));
            Utility.createDirectoryForFile(facetFile);
            ScoredFacet.outputAsFacets(facets, facetFile);
            Utility.infoWritten(facetFile);
        }

        @Override
        public void close() throws IOException {

        }

    }

}
