/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * generate parameters
 */
@Verified
@InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class ExpandQueryWithFeedbacks extends StandardStep<FileName, TfQueryParameters> {

    String runDir;
    ExpansionIdMap expIdMap;
    File newExpIdMapFile;
    HashMap<String, TfQuery> queries;
    HashSet<String> expansions;

    public ExpandQueryWithFeedbacks(TupleFlowParameters parameters) throws IOException {
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
                        String params = Utility.parametersToString(expansion, expId);
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
                String params = Utility.parametersToString(expansion, expId);
                processor.process(new TfQueryParameters(query.id, query.text, params));
            }
            expansions.add(queryExpansion);
        }
    }
}
