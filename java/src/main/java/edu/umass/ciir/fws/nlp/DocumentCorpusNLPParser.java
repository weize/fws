/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.retrieval.CorpusAccessor;
import edu.umass.ciir.fws.retrieval.CorpusAccessorFactory;
import edu.umass.ciir.fws.types.TfDocumentName;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Parse document using PeerPatternNLPParser (used for parsing document in the
 corpus).
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfDocumentName")
public class DocumentCorpusNLPParser implements Processor<TfDocumentName> {

    Parameters parameters;
    PeerPatternNLPParser stanfordParser;
    String parseCorpusDir;
    CorpusAccessor corpusAccessor;

    public DocumentCorpusNLPParser(TupleFlowParameters parameters) throws Exception {
        this.parameters = parameters.getJSON();
        stanfordParser = new PeerPatternNLPParser();
        parseCorpusDir = this.parameters.getString("parseCorpusDir");
        corpusAccessor = CorpusAccessorFactory.instance(this.parameters);
    }

    @Override
    public void process(TfDocumentName docName) throws IOException {
        System.err.println("processing  " + docName.name);
        String outputFileName = corpusAccessor.getParsedDocumentFilename(docName.name);
        // Do not parse again if the parse file already exists
        if (new File(outputFileName).exists()) {
            System.err.println(String.format("Warning: file exists ", outputFileName));
            return;
        }
        
        Utility.createDirectoryForFile(outputFileName);
        
        Document doc = corpusAccessor.getHtmlDocument(docName.name);
        String content = HtmlContentExtractor.extractFromContent(doc.text);
        
        stanfordParser.parse(content , outputFileName);
        System.err.println("Written in " + outputFileName);
    }

    @Override
    public void close() throws IOException {
        corpusAccessor.close();
    }

}
