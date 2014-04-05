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
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * Parse document using Stanford core nlp parser.
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.QueryDocumentName")
public class DocumentNlpParser implements Processor<QueryDocumentName> {
    
    Logger logger;
    
  
    
    public DocumentNlpParser(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        logger = Logger.getLogger(DocumentNlpParser.class.toString());
    }
    
    
    @Override
    public void process(QueryDocumentName docName) throws IOException {
        
    }
    
    
    

    @Override
    public void close() throws IOException {
    }

    
}
