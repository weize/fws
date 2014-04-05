/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.crawl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.core.eval.QueryResults;
import org.lemurproject.galago.core.eval.QuerySetResults;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.ScoredDocument;

/**
 *
 * @author wkong
 */
public class Document {

    public int rank;
    public String name;
    public String html;

    Document(ScoredDocument sd, String text) {
        rank = sd.rank;
        name = sd.documentName;
        html = text;
    }

//    public static Document[] loadDocumentsFromRankedList(String file, String qid, long topNum, Retrieval retrieval) throws IOException {
//        QuerySetResults querySetResults = new QuerySetResults(file);
//        QueryResults queryResults = querySetResults.get(qid);
//
//        int num = 0;
//        boolean full = false;
//        ArrayList<Document> docs = new ArrayList<>();
//        for (ScoredDocument sd : queryResults.getIterator()) {
//            org.lemurproject.galago.core.parse.Document document = retrieval.getDocument(sd.documentName, new org.lemurproject.galago.core.parse.Document.DocumentComponents(true, true, true));
//            docs.add(new Document(sd, document.text));
//            if (++num >= topNum) {
//                full = true;
//                break;
//            }
//        }
//
//        if (!full) {
//            System.err.println("Warning: number of documents is less than requested in rankedlist for query " + qid);
//        }
//
//        return docs.toArray(new Document[0]);
//    }
}
