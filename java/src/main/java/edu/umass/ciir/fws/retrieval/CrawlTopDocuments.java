package edu.umass.ciir.fws.retrieval;

import edu.umass.ciir.fws.clist.*;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.tool.app.ProcessQueryApp;
import edu.umass.ciir.fws.types.TfCandidateList;
import edu.umass.ciir.fws.types.TfQuery;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.lemurproject.galago.core.eval.QueryResults;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * Tupleflow application that crawls top ranked documents for each query. Read
 * from index and write HTML files to document directory.
 *
 * @author wkong
 */
public class CrawlTopDocuments extends ProcessQueryApp {

    @Override
    protected Class getProcessClass() {
        return TopDocumentsWriter.class;
    }

    @Override
    public String getName() {
        return "crawl-top-documents";
    }

    /**
     * From index, fetch HTML content for top ranked documents and save into
     * files, for each query.
     *
     * @author wkong
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    public static class TopDocumentsWriter implements Processor<TfQuery> {

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
        public void process(TfQuery query) throws IOException {
            QueryResults docs = querySetResults.get(query.id);
            String dirName = edu.umass.ciir.fws.utility.Utility.getDocFileDir(docDir, query.id);
            edu.umass.ciir.fws.utility.Utility.createDirectory(dirName);
            for (ScoredDocument sd : docs.getIterator()) {
                org.lemurproject.galago.core.parse.Document doc = retrieval.getDocument(sd.documentName, new org.lemurproject.galago.core.parse.Document.DocumentComponents(true, true, false));
                // html
                File htmlFile = new File(edu.umass.ciir.fws.utility.Utility.getDocHtmlFileName(docDir, query.id, doc.name));
                edu.umass.ciir.fws.utility.Utility.copyStringToFile(doc.text, htmlFile);
                System.err.println(String.format("written in %s", htmlFile.getAbsoluteFile()));

                // data
                File dataFile = new File(edu.umass.ciir.fws.utility.Utility.getDocDataFileName(docDir, query.id, doc.name));
                edu.umass.ciir.fws.utility.Utility.copyStreamToFile(new ByteArrayInputStream(org.lemurproject.galago.core.parse.Document.serialize(doc, new Parameters())), dataFile);
                System.err.println(String.format("written in %s", dataFile.getAbsoluteFile()));

            }
            logger.info(String.format("written down %d documents for query %s", docs.size(), query.id));
        }

        @Override
        public void close() throws IOException {
            retrieval.close();
        }
    }

}
