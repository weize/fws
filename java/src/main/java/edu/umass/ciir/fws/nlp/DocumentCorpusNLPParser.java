/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.types.DocumentName;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.Document.DocumentComponents;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Parse document using StanfordCoreNLPParser (used for parsing document in the
 * corpus).
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.DocumentName")
public class DocumentCorpusNLPParser implements Processor<DocumentName> {

    Parameters parameters;
    StanfordCoreNLPParser stanfordParser;
    String parseCorpusDir;
    Retrieval retrieval;

    public DocumentCorpusNLPParser(TupleFlowParameters parameters) throws Exception {
        this.parameters = parameters.getJSON();
        stanfordParser = new StanfordCoreNLPParser();
        parseCorpusDir = this.parameters.getString("parseCorpusDir");
        retrieval = RetrievalFactory.instance(this.parameters);
    }

    @Override
    public void process(DocumentName docName) throws IOException {
        System.err.println("processing  " + docName.name);
        String outputFileName = Utility.getParsedCorpusDocFileName(parseCorpusDir, docName.name);
        Utility.createDirectoryForFile(outputFileName);

        // Do not parse again if the parse file already exists
        //if (new File(outputFileName).exists()) {
        //    System.err.println(String.format("Warning: file exists ", outputFileName));
        //    return;
        //}

        Document doc = retrieval.getDocument(docName.name, new DocumentComponents(true, false, false));
        String content = HtmlContentExtractor.extractFromContent(doc.text);
        
        stanfordParser.parse(content , outputFileName);
        System.err.println("Written in " + outputFileName);
    }

    @Override
    public void close() throws IOException {
        retrieval.close();
    }

}
