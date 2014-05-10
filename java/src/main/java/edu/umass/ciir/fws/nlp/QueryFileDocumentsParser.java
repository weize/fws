/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.crawl.QuerySetResults;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryDocumentName;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * Parses and emits top ranked documents for each queries.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryDocumentName")
public class QueryFileDocumentsParser extends StandardStep<FileName, TfQueryDocumentName> {

    Logger logger;
    QuerySetResults querySetResults;

    long topNum;

    public QueryFileDocumentsParser(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        String file = p.getString("rankedListFile");
        topNum = p.getLong("topNum");

        querySetResults = new QuerySetResults(file, topNum);
        logger = Logger.getLogger(QueryFileDocumentsParser.class.toString());
    }

    @Override
    public void process(FileName fileName) throws IOException {
        TfQuery[] queries = QueryFileParser.loadQueryList(fileName.filename);
        for (TfQuery q : queries) {
            for (ScoredDocument doc : querySetResults.get(q.id).getIterator()) {
                processor.process(new TfQueryDocumentName(q.id, doc.documentName));
            }
        }
    }
}
