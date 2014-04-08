/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.crawl;

import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
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

    QuerySetDocuments querySetDocuments;
    String docDir;
    BufferedWriter writer;
    Logger logger;

    public TopDocumentsWriter(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        docDir = p.getString("docDir");
        p.set("loadDocsFromIndex", true);
        querySetDocuments = new QuerySetDocuments(p);
        logger = Logger.getLogger(TopDocumentsWriter.class.toString());
    }

    @Override
    public void process(Query query) throws IOException {
        List<Document> docs = querySetDocuments.get(query.id);
        String dirName = String.format("%s%s%s", docDir, File.separator, query.id);
        Utility.createDirectory(dirName);
        for (Document doc : docs) {
            String fileName = String.format("%s%s%s.html",
                    dirName, File.separator, doc.name);
            Utility.copyStringToFile(doc.html, new File(fileName));
        }
        logger.info(String.format("written down %d documents for query %s", docs.size(), query.id));
    }

    @Override
    public void close() throws IOException {

    }
}
