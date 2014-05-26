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
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryExpansion;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * Expand query with each single terms in its facets. Used for extract oracle
 * feedback.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansion")
public class ExpandQueryWithSingleFacetTerm extends StandardStep<FileName, TfQueryExpansion> {

    File expFile; // expansion file for kee tracke of qid and its expaions
    String runDir;
    BufferedWriter writer;
    ExpansionIdMap expIdMap;
    File expansionIdFile;
    HashMap<String, TfQuery> queryMap;
    final static String model = "sts"; // single term simple

    public ExpandQueryWithSingleFacetTerm(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        expFile = new File(p.getString("expansionFile"));
        runDir = p.getString("expansionRunDir");
        expansionIdFile = new File(p.getString("expansionIdFile"));
        if (expansionIdFile.exists()) {
            expIdMap = new ExpansionIdMap(expansionIdFile);
        } else {
            expIdMap = new ExpansionIdMap();
        }
        queryMap = QueryFileParser.loadQueryMap(new File(p.getString("queryFile")));
        writer = Utility.getWriter(expFile);
    }

    @Override
    public void process(FileName file) throws IOException {
        List<FacetAnnotation> fas = FacetAnnotation.load(new File(file.filename));
        TfQueryExpansion a;

        for (FacetAnnotation fa : fas) {
            fa.sortFacets();
            String qid = fa.qid;
            String oriQuery = queryMap.get(qid).text;

            int fidx = 0; // index of valid facet
            for (AnnotatedFacet facet : fa.facets) {
                if (facet.isValid()) {
                    for (int tidx = 0; tidx < facet.size(); tidx++) {
                        FeedbackTerm ft = new FeedbackTerm(facet.get(tidx), fidx, tidx);
                        QueryExpansion qe = new QueryExpansion(qid, oriQuery, model, ft.toString(), expIdMap);
                        File runFile = new File(Utility.getExpansionRunFileName(runDir, qe));
                        if (runFile.exists()) {
                            System.err.println("exists results for " + runFile.getAbsolutePath());
                        } else {
                            qe.expand();
                            processor.process(qe.toTfQueryExpansion());
                        }
                        writer.write(qe.toString());
                    }
                    fidx++;
                }
            }
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
