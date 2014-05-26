/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import static edu.umass.ciir.fws.ffeedback.ExpandQueryWithSingleFacetTerm.model;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryExpansion;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
 * Expand query with facet feedbacks.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansion")
public class ExpandQueryWithFeedbacks extends StandardStep<FileName, TfQueryExpansion> {

    File expFile; // expansion file for kee tracke of qid and its expaions
    String runDir;
    ExpansionIdMap expIdMap;
    File expansionIdFile;
    String model;
    HashMap<String, TfQuery> queryMap;
    HashSet<String> expansions;
    BufferedWriter writer;

    public ExpandQueryWithFeedbacks(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        expFile = new File(p.getString("expansionFile"));
        runDir = p.getString("expansionRunDir");
        model = p.getString("expansionModel");
        expansionIdFile = new File(p.getString("expansionIdFile"));
        if (expansionIdFile.exists()) {
            expIdMap = new ExpansionIdMap(expansionIdFile);
        } else {
            expIdMap = new ExpansionIdMap();
        }
        queryMap = QueryFileParser.loadQueryMap(new File(p.getString("queryFile")));
        writer = Utility.getWriter(expFile);
        expansions = new HashSet<>();
    }

    @Override
    public void process(FileName fileName) throws IOException {
        BufferedReader reader = Utility.getReader(fileName.filename);
        String line;
        while ((line = reader.readLine()) != null) {
            FacetFeedback ff = FacetFeedback.parseFromStringAndSort(line);
            String qid = ff.qid;
            String oriQuery = queryMap.get(qid).text;

            // each time append a feedback term, and do expansion
            ArrayList<FeedbackTerm> selected = new ArrayList<>();
            //expand(qid, oriQuery, selected); // emit one with out expansion
            for (FeedbackTerm term : ff.terms) {
                selected.add(term);
                expand(qid, oriQuery, selected);
            }

        }
        reader.close();
    }

    private void expand(String qid, String oriQuery, ArrayList<FeedbackTerm> selected) throws IOException {
        String expansion = FacetFeedback.toExpansionString(selected);
        String queryExpansion = qid + "\t" + expansion;
        if (!expansions.contains(queryExpansion)) {
            expansions.add(queryExpansion);
            QueryExpansion qe = new QueryExpansion(qid, oriQuery, model, expansion, expIdMap);
            File runFile = new File(Utility.getExpansionRunFileName(runDir, qe));
            if (runFile.exists()) {
                System.err.println("exists results for " + runFile.getAbsolutePath());
            } else {
                qe.expand();
                processor.process(qe.toTfQueryExpansion());
            }
            writer.write(qe.toString());
        }
    }

    @Override
    public void close() throws IOException {
        processor.close();
        writer.close();
        Utility.infoWritten(expFile);
        expIdMap.output(expansionIdFile); // update ids
        Utility.infoWritten(expansionIdFile);
    }
}
