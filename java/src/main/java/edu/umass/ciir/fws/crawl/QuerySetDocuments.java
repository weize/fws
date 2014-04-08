/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.crawl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.core.eval.QueryResults;
import org.lemurproject.galago.core.eval.QuerySetResults;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Utility;

/**
 * Represents retrieved documents for each queries.
 *
 * @author wkong
 */
public class QuerySetDocuments {

    HashMap<String, List<Document>> querySetDocuments;
    HashMap<String, List<ScoredDocument>> querySetResults;
    String rankedListFile;
    Retrieval retrieval;
    String docDir;
    boolean loadDocsFromIndex;

    public QuerySetDocuments(Parameters parameters) throws Exception {
        assert (parameters.isString("index")) : "missing --index";
        assert (parameters.isString("rankedListFile")) : "missing --rankedListFile";
        assert (parameters.isLong("topNum")) : "missing --topNum";

        rankedListFile = parameters.getString("rankedListFile");
        querySetDocuments = new HashMap<>();

        loadQuerySetDocuments(parameters);
    }

    private void loadQuerySetDocuments(Parameters parameters) throws Exception {
        querySetDocuments.clear();
        loadDocsFromIndex = parameters.get("loadDocsFromIndex", false);
        long topNum = parameters.getLong("topNum");
        querySetResults = loadQuerySetResults(rankedListFile, topNum);

        if (loadDocsFromIndex) {
            retrieval = RetrievalFactory.instance(parameters);
            loadQuerySetDocumentFromIndex();
            retrieval.close();
        } else {
            docDir = parameters.getString("docDir");
            loadQuerySetDocumentFromFiles();
        }
    }


    public List<Document> get(String id) {
        return querySetDocuments.get(id);
    }

    /**
     * Load top documents as ScoredDocument
     *
     * @param rankedListFile
     * @param topNum
     * @return
     * @throws IOException
     */
    public static HashMap<String, List<ScoredDocument>> loadQuerySetResults(String rankedListFile, long topNum) throws IOException {
        HashMap<String, List<ScoredDocument>> querySetResults = new HashMap<>();
        QuerySetResults querySetResultsFull = new QuerySetResults(rankedListFile);

        for (String qid : querySetResultsFull.getQueryIterator()) {
            QueryResults queryResultsFull = querySetResultsFull.get(qid);
            ArrayList<ScoredDocument> queryResults = new ArrayList<>();
            // construct Documents from ScoreDocuments for each query
            int num = 0; // only using topNum documents
            boolean full = false;
            for (ScoredDocument sd : queryResultsFull.getIterator()) {
                queryResults.add(sd);
                if (++num >= topNum) {
                    full = true;
                    break;
                }
            }
            if (!full) {
                System.err.print(String.format("warning only %d documents found for query %s\n", num, qid));
            }
            querySetResults.put(qid, queryResults);
        }
        return querySetResults;
    }

    /**
     * Load from index.
     * @throws Exception 
     */
    private void loadQuerySetDocumentFromIndex() throws Exception {
        for (String qid : querySetResults.keySet()) {
            List<ScoredDocument> queryResults = querySetResults.get(qid);
            ArrayList<Document> queryDocuments = new ArrayList<>();
            for (ScoredDocument sd : queryResults) {
                org.lemurproject.galago.core.parse.Document document = retrieval.getDocument(sd.documentName, new org.lemurproject.galago.core.parse.Document.DocumentComponents(true, true, true));
                queryDocuments.add(new Document(sd, document.text));
            }
            querySetDocuments.put(qid, queryDocuments);
        }
    }

    /**
     * Load from files.
     * @throws Exception 
     */
    private void loadQuerySetDocumentFromFiles( ) throws Exception {
        for (String qid : querySetResults.keySet()) {
            List<ScoredDocument> queryResults = querySetResults.get(qid);
            ArrayList<Document> queryDocuments = new ArrayList<>();
            for (ScoredDocument sd : queryResults) {
                String fileName = String.format("%s%s%s%s%s.html",
                        docDir, File.separator, qid, File.separator, sd.documentName);
                String html = Utility.readFileToString(new File(fileName));
                queryDocuments.add(new Document(sd, html));
            }
            querySetDocuments.put(qid, queryDocuments);
        }
    }

}
