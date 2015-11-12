/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.demo.search;

import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.query.StructuredQuery;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class GalagoSearchEngine implements SearchEngine {

    Retrieval retrieval;
    Parameters p;

    public GalagoSearchEngine(Parameters p) {
        this.p = p;
        try {
            retrieval = RetrievalFactory.instance(p);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<RankedDocument> getRankedDocuments(TfQuery query, int top) {
        Utility.info("search via Galago");
        List<ScoredDocument> scoredDocuments = retreive(query, top);
        Utility.info("#top webpages: " + scoredDocuments.size());
        Utility.info("crawl top results");
        List<RankedDocument> rankedDocuments = crawl(scoredDocuments);
        return rankedDocuments;
    }

    public List<ScoredDocument> retreive(TfQuery query, int top) {
        String queryText = toGalagoQueryLanguage(query.text);
        Node root = StructuredQuery.parse(queryText);
        Node transformed;
        List<ScoredDocument> scoredDocuments;

        try {
            transformed = retrieval.transformQuery(root, p);
            scoredDocuments = retrieval.executeQuery(transformed, p).scoredDocuments;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return scoredDocuments.subList(0, Math.min(top, scoredDocuments.size()));
    }

    private String toGalagoQueryLanguage(String text) {
        text = TextProcessing.clean(text);
        return String.format("#sdm( %s )", text);
    }

    private List<RankedDocument> crawl(List<ScoredDocument> scoredDocuments) {

        ArrayList<RankedDocument> docs = new ArrayList<>();
        for (ScoredDocument sd : scoredDocuments) {
            Document doc;

            try {
                doc = retrieval.getDocument(sd.documentName, new Document.DocumentComponents(true, true, false));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            docs.add(toRankedDocument(sd, doc));

//            // html
//            File htmlFile = new File(edu.umass.ciir.fws.utility.Utility.getDocHtmlFileName(docDir, query.id, doc.name));
//            edu.umass.ciir.fws.utility.Utility.copyStringToFile(doc.text, htmlFile);
//            System.err.println(String.format("written in %s", htmlFile.getAbsoluteFile()));
//
//            // data
//            File dataFile = new File(edu.umass.ciir.fws.utility.Utility.getDocDataFileName(docDir, query.id, doc.name));
//            edu.umass.ciir.fws.utility.Utility.copyStreamToFile(new ByteArrayInputStream(org.lemurproject.galago.core.parse.Document.serialize(doc, new Parameters())), dataFile);
//            System.err.println(String.format("written in %s", dataFile.getAbsoluteFile()));
        }
        return docs;
    }

    private RankedDocument toRankedDocument(ScoredDocument sd, Document doc) {
        return new RankedDocument(sd, doc);
    }

    public long getDocFreq(String term) {
        String query = String.format("#od:1( %s )", term);
        Node parsed = StructuredQuery.parse(query);
        parsed.getNodeParameters().set("queryType", "count");
        try {
            Node transformed = retrieval.transformQuery(parsed, new Parameters());
            long count = retrieval.getNodeStatistics(transformed).nodeDocumentCount;
            return count;

        } catch (Exception ex) {
            System.err.println("warning: failed to get docFreq for " + term);
        }
        return -1;
    }

}
