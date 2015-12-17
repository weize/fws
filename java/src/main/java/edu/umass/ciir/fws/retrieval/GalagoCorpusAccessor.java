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
    String clistCorpusDir;

    public GalagoCorpusAccessor(Parameters p) throws Exception {
        boolean needIndex = p.get("needIndex", true);
        retrieval = needIndex ? RetrievalFactory.instance(p) : null;
        parseCorpusDir = p.get("parseCorpusDir", "");
        clistCorpusDir = p.get("clistCorpusDir", "");
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

    @Override
    public String getSystemName() {
        return "galago";
    }

    @Override
    public String getClistFileName(String docName, String suffix) {
        return Utility.getGalagoCorpusCandidateListFileName(clistCorpusDir, docName, suffix);
    }
}
