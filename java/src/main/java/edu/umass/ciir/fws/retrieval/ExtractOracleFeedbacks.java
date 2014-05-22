/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.retrieval;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.query.StructuredQuery;
import static org.lemurproject.galago.core.tools.apps.BatchSearch.logger;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
public class ExtractOracleFeedbacks extends ProcessQueryParametersApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return ExpendQueryWithFaceTerm.class;
    }

    @Override
    protected Class getProcessClass() {
        return ExpanAndRunQuery.class;
    }

    @Override
    public String getName() {
        return "extract-oracle-feedbacks";
    }

    /**
     * generate parameters
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class ExpendQueryWithFaceTerm extends StandardStep<TfQuery, TfQueryParameters> {

        File facetJsonFile;
        File expTermFile;
        BufferedWriter writer;

        public ExpendQueryWithFaceTerm(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            facetJsonFile = new File(p.getString("facetAnnotationJson"));
            expTermFile = new File(p.getString("oracleExpandedTerms"));
            writer = Utility.getWriter(expTermFile);
        }

        @Override
        public void process(TfQuery query) throws IOException {
            HashMap<String, FacetAnnotation> annotations = FacetAnnotation.loadAsMap(facetJsonFile);

            if (annotations.containsKey(query.id)) {
                for (AnnotatedFacet facet : annotations.get(query.id).facets) {
                    if (facet.isValid()) {
                        for (int i = 0; i < facet.size(); i++) {
                            String term = facet.items.get(i).item;
                            String parameters = Utility.parametersToString(facet.fid, "" + i, term);
                            processor.process(new TfQueryParameters(query.id, query.text, parameters));
                            System.err.println(parameters);
                            writer.write(String.format("%s\t%s\t%s-%d\t%s\n", query.id, query.text, facet.fid, i, term));
                        }
                    }
                }
            }

        }

        @Override
        public void close() throws IOException {
            writer.close();
            Utility.infoWritten(expTermFile);
            processor.close();
        }

    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class ExpanAndRunQuery implements Processor<TfQueryParameters> {

        String runDir;
        Retrieval retrieval;
        Parameters p;

        public ExpanAndRunQuery(TupleFlowParameters parameters) throws Exception {
            p = parameters.getJSON();
            runDir = p.getString("oracleExpansionRunDir");
            retrieval = RetrievalFactory.instance(p);
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            String[] params = Utility.splitParameters(queryParams.parameters);
            String fid = params[0];
            String tid = params[1];

            String term = params[2];
            String queryNumber = queryParams.id + "-" + fid + "-" + tid;
            File outfile = new File(Utility.getOracleExpandRunFileName(runDir, queryParams.id, queryNumber));
            Utility.createDirectoryForFile(outfile);
            BufferedWriter writer = Utility.getWriter(outfile);
            String queryText = expandSdmQuery(queryParams.text, term);
            System.err.println(queryNumber + "\t" + queryText);

            // parse and transform query into runnable form
            List<ScoredDocument> results = null;

            Node root = StructuredQuery.parse(queryText);
            Node transformed;
            try {
                transformed = retrieval.transformQuery(root, p);
                // run query
                //results = retrieval.executeQuery(transformed, p).scoredDocuments;
            } catch (Exception ex) {
                Logger.getLogger(ExtractOracleFeedbacks.class.getName()).log(Level.SEVERE, "error in running for"
                        + queryParams.toString(), ex);
            }

            // if we have some results -- print in to output stream
            if (!results.isEmpty()) {
                for (ScoredDocument sd : results) {
                    writer.write(sd.toTRECformat(queryNumber));
                    writer.newLine();
                }
            }
            writer.close();
            Utility.infoWritten(outfile);
        }

        @Override
        public void close() throws IOException {
        }

        private String expandSdmQuery(String text, String term) {
            return String.format("#combine:0=0.6:1=0.4(#sdm( %s ) #combine( %s ))", text, term);
        }

    }

}
