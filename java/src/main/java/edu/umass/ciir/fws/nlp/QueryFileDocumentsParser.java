/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.QueryDocumentName;
import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;
import org.lemurproject.galago.core.eval.QueryResults;
import org.lemurproject.galago.core.eval.QuerySetResults;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * Parse query file and fetch top ranked documents for all queries.
 * Documents emitted are distinct.
 * @author wkong
 */
@Verified
@InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
@OutputClass(className = "edu.umass.ciir.fws.types.DocumentName")
public class QueryFileDocumentsParser extends StandardStep<FileName, QueryDocumentName> {
    
    HashSet<String> docNames;
    Logger logger;
    QuerySetResults querySetResults;
    
    long topNum;
    
    public QueryFileDocumentsParser(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        String file = p.get("rankedListFile", "");
        topNum = p.get("topNum", 100);
        
        docNames = new HashSet<>();
        querySetResults = new QuerySetResults(file);

        logger = Logger.getLogger(QueryFileDocumentsParser.class.toString());
    }
    

    @Override
    public void process(FileName fileName) throws IOException {
        Query [] queries = QueryFileParser.loadQueryList(fileName.filename);
        for(Query q : queries) {
            processPerQuery(q.id);
        }
    }

    private void processPerQuery(String qid) {
        QueryResults documents = querySetResults.get(qid);

        boolean full = false;
        long num = 0;
        for (ScoredDocument doc : documents.getIterator()) {
            docNames.add(doc.documentName);
            if (++num >= topNum) {
                break;
            }
        }

        if (!full) {
            logger.info(String.format("warning only %d documents found for query %s", num, qid));
        }
    }
    
    @Override
    public void close() throws IOException {
        for(String name : docNames) {
            processor.process(new QueryDocumentName(name,"test"));
        }
    }
}
