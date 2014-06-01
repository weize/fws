package edu.umass.ciir.fws.clustering.plsa;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
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
public class PlsaClusterToFacet extends ProcessQueryParametersApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GeneratePlsaFacetParameters.class;
    }

    @Override
    protected Class getProcessClass() {
        return PlsaClusterToFacetConverter.class;
    }

    @Override
    public String getName() {
        return "facet-plsa";
    }

    /**
     * generate parameters
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GeneratePlsaFacetParameters extends StandardStep<TfQuery, TfQueryParameters> {

        List<Long> plsaTopicNums;
        List<Long> plsaTermNums;
        String facetDir;

        public GeneratePlsaFacetParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            String runDir = p.getString("plsaRunDir");
            facetDir = Utility.getFileName(runDir, "facet");
            plsaTopicNums = p.getList("plsaTopicNums");
            plsaTermNums = p.getList("plsaTermNums");
        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (long plsaTopicNum : plsaTopicNums) {
                for (long plsaTermNum : plsaTermNums) {
                    File facetFile = new File(Utility.getPlsaFacetFileName(facetDir, query.id, plsaTopicNum, plsaTermNum));
                    if (facetFile.exists()) {
                        Utility.infoFileExists(facetFile);
                    } else {
                        String parameters = edu.umass.ciir.fws.utility.Utility.parametersToString(plsaTopicNum, plsaTermNum);
                        processor.process(new TfQueryParameters(query.id, query.text, parameters));
                    }
                }
            }

        }

    }

    /**
     *
     * Use first terms in the cluster as facet terms.
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class PlsaClusterToFacetConverter implements Processor<TfQueryParameters> {

        String facetDir;
        String clusterDir;

        public PlsaClusterToFacetConverter(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            String runDir = p.getString("plsaRunDir");
            facetDir = Utility.getFileName(runDir, "facet");
            clusterDir = Utility.getFileName(runDir, "cluster");
        }

        @Override
        public void process(TfQueryParameters queryParameters) throws IOException {
            System.err.println(String.format("Processing qid:%s parameters:%s", queryParameters.id, queryParameters.parameters));
            String qid = queryParameters.id;
            String[] fields = edu.umass.ciir.fws.utility.Utility.splitParameters(queryParameters.parameters);
            long plsaTopicNum = Long.parseLong(fields[0]);
            long plsaTermNum = Long.parseLong(fields[1]);

            File facetFile = new File(Utility.getPlsaFacetFileName(facetDir, qid, plsaTopicNum, plsaTermNum));
            if (facetFile.exists()) {
                Utility.infoFileExists(facetFile);
                return;
            }

            // loadClusters clusters
            File clusterFile = new File(Utility.getPlsaClusterFileName(clusterDir, qid, plsaTopicNum));
            List<ScoredFacet> clusters = ScoredFacet.loadClusters(clusterFile);

            // select facet terms
            for (ScoredFacet cluster : clusters) {
                int size = (int) Math.min(plsaTermNum, cluster.items.size());
                cluster.items = cluster.items.subList(0, size);
            }

            Utility.infoOpen(facetFile);
            Utility.createDirectoryForFile(facetFile);
            ScoredFacet.outputAsFacets(clusters, facetFile);
            Utility.infoWritten(facetFile);
        }

        @Override
        public void close() throws IOException {

        }

    }

}
