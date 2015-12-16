/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.retrieval;

import edu.umass.ciir.fws.utility.Utility;
import java.io.IOException;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * html accessed from index
 * prase accessed from local
 * @author wkong
 */
public class GalagoCorpusAccessor implements CorpusAccessor {

    Retrieval retrieval;
    String parseCorpusDir;

    public GalagoCorpusAccessor(Parameters p) throws Exception {
        retrieval = RetrievalFactory.instance(p);
        parseCorpusDir = p.getString("parseCorpusDir");
    }

    @Override
    public Document getHtmlDocument(String name) throws IOException {
        Document doc = retrieval.getDocument(name, new Document.DocumentComponents(true, false, false));
        return doc;
    }

    @Override
    public String getParsedDocumentFilename(String name) {
        return Utility.getParsedGalagoCorpusDocFileName(parseCorpusDir, name);
    }

    @Override
    public void close() throws IOException{
        retrieval.close();
    }
}
