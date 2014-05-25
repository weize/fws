/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback.oracle;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
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
public class RunAllFacetTermExpasion extends ProcessQueryParametersApp {

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
        return "run-all-facet-term-expansion";
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
        String runDir;
        BufferedWriter writer;
        ExpandTermIdMap expTermMap;
        File newTermIdMapFile;

        public ExpendQueryWithFaceTerm(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            facetJsonFile = new File(p.getString("facetAnnotationJson"));
            expTermFile = new File(p.getString("oracleExpandedTerms"));
            runDir = p.getString("oracleExpansionRunDir");
            if (p.containsKey("oracleExpandedTermIdMapOld")) {
                expTermMap = new ExpandTermIdMap(new File(p.getString("oracleExpandedTermIdMapOld")));
            } else {
                expTermMap = new ExpandTermIdMap();
            }
            newTermIdMapFile = new File(p.getString("oracleExpandedTermIdMap"));
            writer = Utility.getWriter(expTermFile);
            writer.write("#qid\ttermId\tfid-tid\tquery\tterm\n");

        }

        @Override
        public void process(TfQuery query) throws IOException {
            HashMap<String, FacetAnnotation> annotations = FacetAnnotation.loadAsMap(facetJsonFile);

            if (annotations.containsKey(query.id)) {
                FacetAnnotation fa = annotations.get(query.id);
                fa.sortFacets();
                int fidx = 0;
                for (AnnotatedFacet facet : fa.facets) {
                    if (facet.isValid()) {
                        for (int i = 0; i < facet.size(); i++) {
                            String term = facet.get(i);
                            Integer id = expTermMap.getId(query.id, term);
                            File runFile = new File(Utility.getOracleExpandRunFileName(runDir, query.id, id));
			    if (runFile.exists()) {
				System.err.println("exists results for " + runFile.getAbsolutePath());
			    } else {
				    String parameters = Utility.parametersToString(id, term);
                                processor.process(new TfQueryParameters(query.id, query.text, parameters));
                            }
                            writer.write(String.format("%s\t%d\t%d\t%d\t%s\t%s\t%s\n", query.id, id, fidx, i, facet.fid, query.text, term));
                        }
                        fidx ++;
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {
            writer.close();
            Utility.infoWritten(expTermFile);
            processor.close();
            expTermMap.output(newTermIdMapFile); // update ids
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
            int id = Integer.parseInt(params[0]);
            String term = params[1];

            String queryNumber = queryParams.id; // used in the rank results
            File outfile = new File(Utility.getOracleExpandRunFileName(runDir, queryParams.id, id));
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
                results = retrieval.executeQuery(transformed, p).scoredDocuments;
            } catch (Exception ex) {
                Logger.getLogger(RunAllFacetTermExpasion.class.getName()).log(Level.SEVERE, "error in running for"
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
