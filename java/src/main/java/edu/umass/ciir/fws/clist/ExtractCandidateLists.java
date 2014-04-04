package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.query.*;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * *
 *
 * @author wkong
 */
public class ExtractCandidateLists extends AppFunction {

    private static final String name = "extract-candidate-lists";
    String queryFile;
    String rankedListFile;
    String outputDir;
    long topNum; // top number of documents in the rank for extracting candidate lists
    
    Query [] queries;
    Retrieval retrieval;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHelpString() {
        return "fws " + name + " <parameters>+: \n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        assert(p.isString("queryFile")) : "missing input file, --queryFile";
        assert(p.isString("rankedListFile")) : "missing output file, --rankedListFile";
        assert(p.isString("clistDir")) : "missing --clistDir";
        assert(p.isString("index")) : "missing --clistDir";

        queryFile = p.get("queryFile", "");
        rankedListFile = p.get("rankedListFile", "");
        outputDir = p.get("clistDir", "");
        topNum = p.get("topNum", 100);

        // load queries
        queries = Query.loadQueryList(queryFile);
        // load index
        retrieval = RetrievalFactory.instance(p);
        
        for(Query q: queries) {
            
            process(q);
        }
  
    }

    private void process(Query query) throws IOException {
        // load top documents in the rank for the query
        Document [] docs = Document.loadDocumentsFromRankedList(rankedListFile, query.id, topNum, retrieval);
        for (Document doc : docs) {
            System.out.println(doc.name);
            System.out.println(doc.html);
        }
        
    }
}
