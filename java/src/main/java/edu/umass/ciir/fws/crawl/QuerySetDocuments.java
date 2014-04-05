/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.crawl;

import java.io.File;
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
 * Top ranked documents for each queries.
 *
 * @author wkong
 */
public class QuerySetDocuments {

    HashMap<String, List<Document>> querySetDocuments;
    String rankedListFile;
    long topNum;
    QuerySetResults querySetResults;
    Retrieval retrieval;
    String docDir;
    boolean loadDocsFromIndex;

    public QuerySetDocuments(Parameters parameters) throws Exception {
        assert (parameters.isString("index")) : "missing --index";
        assert (parameters.isString("rankedListFile")) : "missing --rankedListFile";
        assert (parameters.isLong("topNum")) : "missing --topNum";

        rankedListFile = parameters.getString("rankedListFile");
        topNum = parameters.getLong("topNum");
        querySetDocuments = new HashMap<>();

        loadQuerySetDocuments(parameters);
    }

    private void loadQuerySetDocuments(Parameters parameters) throws Exception {
        loadDocsFromIndex = parameters.get("loadDocsFromIndex", false);
        querySetResults = new QuerySetResults(rankedListFile);

        if (loadDocsFromIndex) {
            retrieval = RetrievalFactory.instance(parameters);
            load(parameters);
            retrieval.close();
        } else {
            docDir = parameters.getString(docDir);
            load(parameters);
        }
    }

    private void load(Parameters parameters) throws Exception {
        for (String qid : querySetResults.getQueryIterator()) {
            QueryResults queryResults = querySetResults.get(qid);
            // construct Documents from ScoreDocuments for each query
            int num = 0; // only using topNum documents
            boolean full = false;
            ArrayList<Document> queryDocuments = new ArrayList<>();
            for (ScoredDocument sd : queryResults.getIterator()) {
                String html;
                if (loadDocsFromIndex) {
                    org.lemurproject.galago.core.parse.Document document = retrieval.getDocument(sd.documentName, new org.lemurproject.galago.core.parse.Document.DocumentComponents(true, true, true));
                    html = document.text;
                } else { // read from files
                    String fileName = String.format("%s%s%s%s%s.html", 
                            docDir, File.separator, qid, File.separator, sd.documentName);
                    html = Utility.readFileToString(new File(fileName));
                }
                queryDocuments.add(new Document(sd, html));
                if (++num >= topNum) {
                    full = true;
                    break;
                }
            }

            if (!full) {
                System.err.print(String.format("warning only %d documents found for query %s\n", num, qid));
            }
            querySetDocuments.put(qid, queryDocuments);
        }
    }

    public List<Document> get(String id) {
        return querySetDocuments.get(id);
    }

}
