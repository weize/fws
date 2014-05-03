package edu.umass.ciir.fws.clustering.lda;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.types.QueryParameters;
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
 * Tupleflow application that reads in lda clusters and selects top N terms in
 * each cluster, and outputs query facets.
 *
 *
 * @author wkong
 */
public class LdaClusterToFacet extends ProcessQueryParametersApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateLdaFacetParameters.class;
    }

    @Override
    protected Class getProcessClass() {
        return LdaClusterToFacetConverter.class;
    }

    @Override
    public String getName() {
        return "facet-lda";
    }

    /**
     * generate parameters
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.Query")
    @OutputClass(className = "edu.umass.ciir.fws.types.QueryParameters")
    public static class GenerateLdaFacetParameters extends StandardStep<Query, QueryParameters> {

        List<Long> ldaTopicNums;
        List<Long> ldaTermNums;

        public GenerateLdaFacetParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            ldaTopicNums = p.getList("ldaTopicNums");
            ldaTermNums = p.getList("ldaTermNums");
        }

        @Override
        public void process(Query query) throws IOException {
            for (long plsaTopicNum : ldaTopicNums) {
                for (long plsaTermNum : ldaTermNums) {
                    String parameters = Utility.parametersToString(plsaTopicNum, plsaTermNum);
                    processor.process(new QueryParameters(query.id, query.text, parameters));
                }
            }

        }

    }

    /**
     *
     * Use first terms in the cluster as facet terms.
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.QueryParameters")
    public static class LdaClusterToFacetConverter implements Processor<QueryParameters> {

        String facetDir;
        String clusterDir;

        public LdaClusterToFacetConverter(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            facetDir = p.getString("ldaFacetDir");
            clusterDir = p.getString("ldaClusterDir");
        }

        @Override
        public void process(QueryParameters queryParameters) throws IOException {
            System.err.println(String.format("Processing qid:%s parameters:%s", queryParameters.id, queryParameters.parameters));
            String qid = queryParameters.id;
            String[] fields = Utility.splitParameters(queryParameters.parameters);
            long topicNum = Long.parseLong(fields[0]);
            long termNum = Long.parseLong(fields[1]);

            // load clusters
            File clusterFile = new File(Utility.getLdaClusterFileName(clusterDir, qid, topicNum));
            List<ScoredFacet> clusters = ScoredFacet.load(clusterFile);

            // select facet terms
            for (ScoredFacet cluster : clusters) {
                int size = (int) Math.min(termNum, cluster.items.size());
                cluster.items = cluster.items.subList(0, size);
            }
            File facetFile = new File(Utility.getLdaFacetFileName(facetDir, qid, topicNum, termNum));
            Utility.createDirectoryForFile(facetFile);
            ScoredFacet.outputAsFacets(clusters, facetFile);
            Utility.infoWritten(facetFile);
        }

        @Override
        public void close() throws IOException {

        }

    }
}
