/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.crawl;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.nlp.HtmlContentExtractor;
import edu.umass.ciir.fws.utility.TextProcessing;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.core.retrieval.ScoredDocument;

/**
 * Represents a document.
 *
 * @author wkong
 */
public class Document {

    public long rank;
    public String name;
    public String html;
    public String title;
    public String url;
    public String site;
    public List<String> terms;
    public HashMap<String, Integer> ngramMap; // ngram -> frequency

    Document(ScoredDocument sd, org.lemurproject.galago.core.parse.Document document) {
        rank = sd.rank;
        name = sd.documentName;
        html = document.text;
        this.url = document.metadata.get("url");
        //this.terms = document.terms; 
        // galago tokenzier is inconsistent when processing original html and the text content of it,
        // for single quote. "&apos;" will be a split point in galago tokenizer, but "\'" is not.
        // So I re-tokenize by the html's content.
        this.terms = TextProcessing.tokenize(HtmlContentExtractor.extractFromContent(document.text));
        site = getSiteUrl(url);
        title = TextProcessing.clean(HtmlContentExtractor.extractTitle(document.text));

    }

    Document(ScoredDocument sd, String text) {
        rank = sd.rank;
        name = sd.documentName;
        html = text;
    }

    public static String getSiteUrl(String url) {
        url = url.replaceAll("^https?://", "");
        url = url.replaceAll("/.*?$", "");
        url = url.replaceAll("\\|", "");
        url = url.toLowerCase();
        return url;
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
