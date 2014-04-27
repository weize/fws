/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.crawl;

import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.lemurproject.galago.core.eval.QueryResults;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * From index, fetch HTML content for top ranked documents and save into files,
 * for each query.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.Query")
public class TopDocumentsWriter implements Processor<Query> {

    Retrieval retrieval;
    QuerySetResults querySetResults;
    String docDir;
    String rankedListFile;
    long topNum;
    BufferedWriter writer;
    Logger logger;

    public TopDocumentsWriter(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        docDir = p.getString("docDir");
        rankedListFile = p.getString("rankedListFile");
        topNum = p.getLong("topNum");
        querySetResults = new QuerySetResults(rankedListFile, topNum);
        retrieval = RetrievalFactory.instance(p);
        logger = Logger.getLogger(TopDocumentsWriter.class.toString());
    }

    @Override
    public void process(Query query) throws IOException {
        QueryResults docs = querySetResults.get(query.id);
        String dirName = Utility.getDocDirName(docDir, query.id);
        Utility.createDirectory(dirName);
        for (ScoredDocument sd : docs.getIterator()) {
            Document doc = retrieval.getDocument(sd.documentName, new Document.DocumentComponents(true, true, false));
            // html
            File htmlFile = new File(Utility.getDocFileName(docDir, query.id, doc.name, "html"));
            Utility.copyStringToFile(doc.text, htmlFile);
            System.err.println(String.format("written in %s", htmlFile.getAbsoluteFile()));
            
            // data
            File dataFile = new File(Utility.getDocFileName(docDir, query.id, doc.name, "dat"));
            Utility.copyStreamToFile(new ByteArrayInputStream(Document.serialize(doc, new Parameters())), dataFile);
            System.err.println(String.format("written in %s", dataFile.getAbsoluteFile()));

        }
        logger.info(String.format("written down %d documents for query %s", docs.size(), query.id));
    }

    @Override
    public void close() throws IOException {
        retrieval.close();
    }
}
