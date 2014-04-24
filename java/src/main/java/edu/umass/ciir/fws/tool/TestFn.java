/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.tool;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.stanford.nlp.pipeline.PTBTokenizerAnnotator;
import edu.umass.ciir.fws.nlp.HtmlContentExtractor;
import edu.umass.ciir.fws.nlp.StanfordCoreNLPParser;
import edu.umass.ciir.fws.utility.TextProcessing;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.query.StructuredQuery;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Utility;

/**
 *
 * @author wkong
 */
public class TestFn extends AppFunction {

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String getHelpString() {
        return "fws test\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        output.println("in test");
        output.println();

        //testPrintHTML(p, output);
        testNlp(output);
        //testTokenizer(p, output);
        //testReplace(p, output);
        //testPrintTermsInDoc(p, output);

    }

    private void testPrintTermsInDoc(Parameters p, PrintStream output) throws Exception {
        Retrieval retrieval = RetrievalFactory.instance(p);

        String docName = "clueweb09-en0000-02-34845";
        Document document = retrieval.getDocument(docName, new Document.DocumentComponents(true, true, true));

        output.println(docName);

        output.println("================meta================");
        for (String key : document.metadata.keySet()) {
            output.println(key + "\t" + document.metadata.get(key));
        }

        output.println("\n\n================terms================");
        for (String term : document.terms) {
            output.println(term);
        }

        output.println("\n\n================terms2================");
        for (String term : TextProcessing.tokenize(HtmlContentExtractor.extractFromContent(document.text))) {
            output.println(term);
        }

    }

    private void testPrintHTML(Parameters p, PrintStream output) throws Exception {
        Retrieval retrieval = RetrievalFactory.instance(p);
        String query = "#sdm( horse hooves )";
        Node root = StructuredQuery.parse(query);       // turn the query string into a query tree
        Node transformed = retrieval.transformQuery(root, p);  // apply traversals

        List<ScoredDocument> results = retrieval.executeQuery(transformed, p).scoredDocuments;

        for (ScoredDocument sd : results) { // print results
            output.println(sd.documentName);

            Document document = retrieval.getDocument(sd.documentName, new Document.DocumentComponents(true, true, true));
            for (String term : document.terms) {
                output.println(term);
            }
            for (String key : document.metadata.keySet()) {
                output.println(key + "\t" + document.metadata.get(key));
            }
            output.println(document.text);
            break;
        }
    }

    private void testTokenizer(Parameters p, PrintStream output) {
        String text = "U.S.A and U.S. are the © abbrevation_for United States of American.\n"
                + "edu.umass.ciir.fws and edu.umass.cirr.galago are package pathes.\n"
                + "Mom's and dad's computers are34343 234 updated.\n"
                + "We are learning Español and 日本語\n";
        List<String> tokens = TextProcessing.tokenize(text);
        for (String token : tokens) {
            output.println(token);
        }
    }

    private void testNlp(PrintStream output) throws IOException {
        output.println("In test nlp!");
//        PTBTokenizerAnnotator tokenizer;
//        tokenizer = new PTBTokenizerAnnotator();
//        tokenizer.
        StanfordCoreNLPParser stanfordParser = new StanfordCoreNLPParser();
//        String htmlFile = "../exp/doc/51/clueweb09-en0000-02-14693.html";
//        String text = HtmlContentExtractor.extractFromFile(htmlFile);
//        Utility.copyStringToFile(text, new File("test.content"));
        String text = "U.S.A and U.S. are the abbrevation for United States of American.\n"
                + "edu.umass.ciir.fws and edu.umass.cirr.galago are package pathes.\n"
                + "Mom's and dad's computers are updated.\n"
                + "We are learning Español and 日本語.\n"
                + "A and B say \'this is good!\'\n"
                + "we 'ths is a bad case, and we know' fefe.\n";
        stanfordParser.parse(text, "test.parse");
    }

    private void testReplace(Parameters p, PrintStream output) {
        String text = "They   're hi's efe '' efe '6efe";
        output.println(text);
        text = text.replaceAll("\\s+'([\\p{Alnum}])", "'$1");
        output.println(text);
    }

}
