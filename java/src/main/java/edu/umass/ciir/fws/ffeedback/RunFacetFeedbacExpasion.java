/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.tool.app.SplitAndProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 *
 * @author wkong
 */
public class RunFacetFeedbacExpasion extends SplitAndProcessQueryParametersApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return ExpendQueryWithFacetFeedback.class;
    }

    @Override
    protected Class getProcessClass() {
        return ExpanAndRunQuery.class;
    }

    @Override
    public String getName() {
        return "run-ffeedback-expansion";
    }

    @Override
    public String getInputFileName(Parameters parameter) {
        return parameter.getString("feedbackFileName");
    }

    /**
     * generate parameters
     */
    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class ExpendQueryWithFacetFeedback extends StandardStep<FileName, TfQueryParameters> {

        String runDir;
        ExpansionIdMap expIdMap;
        File newExpIdMapFile;
        HashMap<String, TfQuery> queries;
        HashSet<String> expansions;

        public ExpendQueryWithFacetFeedback(TupleFlowParameters parameters) throws IOException {
            Parameters p = parameters.getJSON();
            runDir = p.getString("feedbackExpansionRunDir");
            if (p.containsKey("feedbackExpansionIdMapOld")) {
                expIdMap = new ExpansionIdMap(new File(p.getString("feedbackExpansionIdMapOld")));
            } else {
                expIdMap = new ExpansionIdMap();
            }
            newExpIdMapFile = new File(p.getString("feedbackExpansionIdMap"));
            queries = QueryFileParser.loadQueryMap(new File(p.getString("queryFile")));
            expansions = new HashSet<>();
        }

        @Override
        public void process(FileName fileName) throws IOException {
            BufferedReader reader = Utility.getReader(fileName.filename);
            String line;
            while ((line = reader.readLine()) != null) {
                FacetFeedback ff = FacetFeedback.parseFromString(line);
                TfQuery query = queries.get(ff.qid);

                emitNoExpansion(query);
                
                ArrayList<FeedbackTerm> selected = new ArrayList<>();
                for (FeedbackTerm term : ff.terms) {
                    selected.add(term);
                    String expansion = FacetFeedback.toUniqueExpansionString(selected);
                    String queryExpansion = query.id + "\t" + expansion;
                    if (!expansions.contains(queryExpansion)) {
                        Integer expId = expIdMap.getId(query.id, expansion);
                        File runFile = new File(Utility.getExpansionRunFileName(runDir, query.id, expId));
                        if (runFile.exists()) {
                            System.err.println("exists results for " + runFile.getAbsolutePath());
                        } else {
                            String params = Utility.parametersToString(expId, expansion);
                            processor.process(new TfQueryParameters(query.id, query.text, params));
                        }
                        expansions.add(queryExpansion);
                    }
                }

            }
            reader.close();
        }

        @Override
        public void close() throws IOException {
            processor.close();
            expIdMap.output(newExpIdMapFile); // update ids
        }

        private void emitNoExpansion(TfQuery query) throws IOException {
            String expansion = "";
            String queryExpansion = query.id + "\t" + expansion;
            if (!expansions.contains(queryExpansion)) {
                Integer expId = expIdMap.getId(query.id, expansion);
                File runFile = new File(Utility.getExpansionRunFileName(runDir, query.id, expId));
                if (runFile.exists()) {
                    System.err.println("exists results for " + runFile.getAbsolutePath());
                } else {
                    processor.process(new TfQueryParameters(query.id, query.text, expansion));
                }
                expansions.add(queryExpansion);
            }
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
            runDir = p.getString("feedbackExpansionRunDir");
            retrieval = RetrievalFactory.instance(p);
        }

        @Override
        public void process(TfQueryParameters queryParams) throws IOException {
            String [] params = Utility.splitParameters(queryParams.parameters);
            int expId = Integer.parseInt(params[0]);
            FacetFeedback feedback = FacetFeedback.parseTermsFromUniqueExpansionString(params[1]);
            
            String queryNumber = queryParams.id; // used in the rank results
            File outfile = new File(Utility.getExpansionRunFileName(runDir, queryParams.id, expId));
            Utility.createDirectoryForFile(outfile);
            BufferedWriter writer = Utility.getWriter(outfile);
            String queryText = expandSdmQuery(queryParams.text, feedback);
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
                Logger.getLogger(RunFacetFeedbacExpasion.class.getName()).log(Level.SEVERE, "error in running for"
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

        /**
         * #combine( query #combine( #combine(term1) #combine(term2) ...))
         * @param text
         * @param feedback
         * @return 
         */
        private String expandSdmQuery(String text, FacetFeedback feedback) {
            StringBuilder query = new StringBuilder();
            if (feedback.terms.isEmpty()) {
                return String.format("#sdm( %s )", text);
            } else {
                query.append(String.format("#combine:0=0.8:1=0.2(#sdm( %s ) #combine( ", text));
                for (FeedbackTerm term : feedback.terms) {
                    query.append(String.format("#combine( %s ) ", term.term));
                }
                query.append("))");
            }
            return query.toString();
        }

    }

}
