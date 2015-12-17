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
public class LocalCorpusAccessor implements CorpusAccessor {

    String docCorpusDir;
    String parseCorpusDir;
    String clistCorpusDir;

    public LocalCorpusAccessor(Parameters p) throws Exception {
        docCorpusDir = p.get("docCorpusDir", "");
        parseCorpusDir = p.get("parseCorpusDir", "");
        clistCorpusDir = p.get("clistCorpusDir", "");
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

    
    public static String praseQid(String docName) {
        String[] elems = docName.split("-"); // bing-$qid-$rank
        String qid = elems[1];
        String rank = elems[2];
        return qid;
    }
    
    public static int praseRank(String docName) {
        String[] elems = docName.split("-"); // bing-$qid-$rank
        String qid = elems[1];
        String rank = elems[2];
        return Integer.parseInt(rank);
    }
    
    
    
    @Override
    public String getSystemName() {
        return "bing";
    }

    @Override
    public String getClistFileName(String docName, String suffix) {
        String qid = praseQid(docName);
        return Utility.getFileNameWithSuffix(clistCorpusDir, qid, docName, suffix);
    }

}
