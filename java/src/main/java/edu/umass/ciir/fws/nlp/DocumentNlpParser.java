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
    String parseDir;
    String docDir;

    public DocumentNlpParser(TupleFlowParameters parameters) throws IOException {
        this.parameters = parameters.getJSON();
        logger = Logger.getLogger(DocumentNlpParser.class.toString());
        stanfordParser = new StanfordCoreNLPParser();
        parseDir = this.parameters.getString("parseDir");
        docDir = this.parameters.getString("docDir");
    }

    @Override
    public void process(QueryDocumentName queryDocName) throws IOException {

        String dirName = Utility.getParsedDocDirName(parseDir, queryDocName.qid);
        Utility.createDirectory(dirName);

        String outputFileName = Utility.getParsedDocFileName(
                parseDir, queryDocName.qid, queryDocName.docName);

        // Do not parse again if the parse file already exists
        if (new File(outputFileName).exists()) {
            System.err.println(String.format("Warning: file exists ", outputFileName));
            return;
        }

        String inputFileName = Utility.getDocFileName(
                docDir, queryDocName.qid, queryDocName.docName);
        String content = HtmlContentExtractor.extract(inputFileName);

        System.err.println("processing  " + inputFileName);
        stanfordParser.parse(content, outputFileName);
        System.err.println("Written in " + outputFileName);
    }

    @Override
    public void close() throws IOException {
    }

}
