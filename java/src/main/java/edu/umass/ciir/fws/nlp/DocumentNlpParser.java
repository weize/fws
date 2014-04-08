/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.types.QueryDocumentName;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Parse document using StanfordCoreNLPParser.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.QueryDocumentName")
public class DocumentNlpParser implements Processor<QueryDocumentName> {

    Logger logger;
    Parameters parameters;
    StanfordCoreNLPParser stanfordParser;

    public DocumentNlpParser(TupleFlowParameters parameters) throws IOException {
        this.parameters = parameters.getJSON();
        logger = Logger.getLogger(DocumentNlpParser.class.toString());
        stanfordParser = new StanfordCoreNLPParser();
    }

    @Override
    public void process(QueryDocumentName queryDocName) throws IOException {
        String parseDir = parameters.getString("parseDir");
        String dirName = String.format("%s%s%s", parseDir, File.separator, queryDocName.qid);
        Utility.createDirectory(dirName);

        String outputFileName = String.format("%s%s%s.parse", dirName,
                File.separator, queryDocName.docName);

        // Do not parse again if the parse file already exists
        if (new File(outputFileName).exists()) {
            System.err.println(String.format("Warning: file exists ", outputFileName));
            return;
        }

        String docDir = parameters.getString("docDir");
        String inputFileName = String.format("%s%s%s%s%s.html", docDir, File.separator,
                queryDocName.qid, File.separator, queryDocName.docName);
        String content = HtmlContentExtractor.extract(inputFileName);

        System.err.println("processing  " + inputFileName);
        stanfordParser.parse(content, outputFileName);
        System.err.println("Written in " + outputFileName);
    }

    @Override
    public void close() throws IOException {
    }

}
