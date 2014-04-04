/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

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

    int rank;
    String name;
    String html;

    private Document(ScoredDocument sd, String text) {
        rank = sd.rank;
        name = sd.documentName;
        html = text;
    }

    public static Document[] loadDocumentsFromRankedList(String file, String qid, long topNum, Retrieval retrieval) throws IOException {
        QuerySetResults querySetResults = new QuerySetResults(file);
        QueryResults queryResults = querySetResults.get(qid);

        int num = 0;
        boolean full = false;
        ArrayList<Document> docs = new ArrayList<>();
        for (ScoredDocument sd : queryResults.getIterator()) {
            org.lemurproject.galago.core.parse.Document document = retrieval.getDocument(sd.documentName, new org.lemurproject.galago.core.parse.Document.DocumentComponents(true, true, true));
            docs.add(new Document(sd, document.text));
            if (++num >= topNum) {
                full = true;
                break;
            }
        }

        if (!full) {
            System.err.println("Warning: number of documents is less than requested in rankedlist for query " + qid);
        }

        return docs.toArray(new Document[0]);
    }

    /**
     * *
     * Load top ranked documents for all queries
     *
     * @param file
     * @param topNum
     * @param retrieval
     * @return
     */
    public static HashMap<String, List<Document>> loadQuerySetDocuments(String file, long topNum, Retrieval retrieval) throws IOException {
        HashMap<String, List<Document>> querySetDocuments = new HashMap<>();

        QuerySetResults querySetResults = new QuerySetResults(file);

        for (String qid : querySetResults.getQueryIterator()) {
            QueryResults queryResults = querySetResults.get(qid);
            List<Document> queryDocuments = loadQueryDocuments(qid, queryResults, topNum, retrieval);
            querySetDocuments.put(qid, queryDocuments);
        }
        return querySetDocuments;
    }

    /***
     * load documents for one single query
     * @param qid
     * @param queryResults
     * @param topNum
     * @param retrieval
     * @return
     * @throws IOException 
     */
    private static List<Document> loadQueryDocuments(String qid, QueryResults queryResults, long topNum, Retrieval retrieval) throws IOException {
        int num = 0;
        boolean full = false;
        ArrayList<Document> docs = new ArrayList<>();
        for (ScoredDocument sd : queryResults.getIterator()) {
            org.lemurproject.galago.core.parse.Document document = retrieval.getDocument(sd.documentName, new org.lemurproject.galago.core.parse.Document.DocumentComponents(true, true, true));
            docs.add(new Document(sd, document.text));
            if (++num >= topNum) {
                full = true;
                break;
            }
        }

        if (!full) {
            System.err.println("Warning: number of documents is less than requested in rankedlist for query " + qid);
        }

        return docs;
    }
}
