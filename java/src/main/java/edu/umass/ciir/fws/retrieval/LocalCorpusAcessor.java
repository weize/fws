/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.retrieval;

import edu.umass.ciir.fws.utility.Utility;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * for Bing
 * html in $docCorpusDir/$qid/bing-$qid-$rank.html
 * parse in $parseCorpusDir/$qid/bing-$qid-$rank.parse
 * @author wkong
 */
public class LocalCorpusAcessor implements CorpusAccessor {

    String docCorpusDir;
    String parseCorpusDir;

    public LocalCorpusAcessor(Parameters p) throws Exception {
        docCorpusDir = p.getString("docCorpusDir");
        parseCorpusDir = p.getString("parseCorpusDir");
    }

    @Override
    public Document getHtmlDocument(String name) throws IOException {
        String[] elems = name.split("-"); // bing-$qid-$rank
        String qid = elems[1];
        DataInputStream data = new DataInputStream(new FileInputStream(Utility.getDocDataFileName(docCorpusDir, qid, name)));
        Document doc = Document.deserialize(data, new Parameters(), new Document.DocumentComponents(true, true, false));
        return doc;
    }

    @Override
    public String getParsedDocumentFilename(String name) {
        String[] elems = name.split("-"); // bing-$qid-$rank
        String qid = elems[1];
        return Utility.getParsedDocFileName(parseCorpusDir, qid, name);

    }

    @Override
    public void close() throws IOException {
    }

}
