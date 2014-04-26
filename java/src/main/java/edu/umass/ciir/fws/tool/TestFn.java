/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.tool;

import edu.umass.ciir.fws.clist.CandidateListHtmlExtractor;
import edu.umass.ciir.fws.clist.CandidateListTextExtractor;
import edu.umass.ciir.fws.clist.CandidateListTextExtractor.ParseTree;
import edu.umass.ciir.fws.nlp.HtmlContentExtractor;
import edu.umass.ciir.fws.nlp.StanfordCoreNLPParser;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.core.parse.TagTokenizer;

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
        //testNlp(output);
        //testTokenizer(p, output);
        //testReplace(p, output);
        //testPrintTermsInDoc(p, output);
        //testHtml(p, output);
        //testCandidateListHtmlExtractor(p, output);
        //testHtmlContentExtractor(p, output);
        testCandidateListTextExtractor(p, output);

    }

    private void testPrintTermsInDoc(Parameters p, PrintStream output) throws Exception {
        Retrieval retrieval = RetrievalFactory.instance(p);
        String docName = "clueweb09-en0000-03-33030";
        Document document = retrieval.getDocument(docName, new Document.DocumentComponents(true, true, true));

        output.println(docName);

        output.println("================meta================");
        for (String key : document.metadata.keySet()) {
            output.println(key + "\t" + document.metadata.get(key));
        }
//
//        output.println("\n\n================terms================");
//        for (String term : document.terms) {
//            output.println(term);
//        }

        output.println("\n\n================content================");
        String content = HtmlContentExtractor.extractFromContent(document.text);
        output.println(content);
        output.println("\n\n================terms2================");
        for (String term : TextProcessing.tokenize(HtmlContentExtractor.extractFromContent(content))) {
            output.println(term);
        }

    }

    private void testPrintHTML(Parameters p, PrintStream output) throws Exception {
//        Retrieval retrieval = RetrievalFactory.instance(p);
//        String query = "#sdm( horse hooves )";
//        Node root = StructuredQuery.parse(query);       // turn the query string into a query tree
//        Node transformed = retrieval.transformQuery(root, p);  // apply traversals
//
//        List<ScoredDocument> results = retrieval.executeQuery(transformed, p).scoredDocuments;
//
//        for (ScoredDocument sd : results) { // print results
//            output.println(sd.documentName);
//
//            Document document = retrieval.getDocument(sd.documentName, new Document.DocumentComponents(true, true, true));
//            for (String term : document.terms) {
//                output.println(term);
//            }
//            for (String key : document.metadata.keySet()) {
//                output.println(key + "\t" + document.metadata.get(key));
//            }
//            output.println(document.text);
//            break;
//        }
    }

    private void testTokenizer(Parameters p, PrintStream output) {
//        String text = "U.S.A <tag> and U.S. are efe©egfe the© f©fefe abbrevation_for United States of American.\n"
//                + "edu.umass.ciir.fws and edu.umass.cirr.galago are package pathes.\n"
//                + "Mom's and dad's computers are34343 234 updated.\n"
//                + "We are learning ““ “fef Español and 日本語\n"
//                + "We’ve d‘one!\n"
//                + "that`efe `` efe\n"
//                + "17.1 h\n";

        String text = "17.1 h";
        output.println("================term1===========\n");
        List<String> tokens = TextProcessing.tokenize(text);
        for (String token : tokens) {
            output.println(token);
        }

        output.println("\n\n================term2===========\n");
        TagTokenizer tokenizer = new TagTokenizer();
        Document doc = new Document();
        doc.text = text;
        tokenizer.tokenize(doc);
        for (String token : doc.terms) {
            output.println(token);
        }
    }

    private void testNlp(PrintStream output) throws IOException {
        output.println("In test nlp!");
        StanfordCoreNLPParser stanfordParser = new StanfordCoreNLPParser();
        String text = "U.S.A and U.S. are the abbrevation for United States of American.\n"
                + "edu.umass.ciir.fws and edu.umass.cirr.galago are package pathes.\n"
                + "Mom's and dad's computers are updated.\n"
                + "Mom’s and dad’s computers are updated.\n"
                + "Mom`s and dad`s computers are updated.\n"
                + "We are learning Español and 日本語.\n"
                + "A and B say “this is good!“\n"
                + "A and B say ‘this is good!’\n"
                + "we 'ths is a bad case, and we know' fefe.\n"
                + "and efe©egfe efe ©egfe.\n"
                + "We say \"efe©egfe and ©gfe\".\n";
        stanfordParser.parse(text, "test.parse");
    }

    private void testReplace(Parameters p, PrintStream output) {
        String text = "They   're hi's efe '' efe '6efe";
        output.println(text);
        text = text.replaceAll("\\s+'([\\p{Alnum}])", "'$1");
        output.println(text);
    }

    private void testHtml(Parameters p, PrintStream output) {
        String html = "<html><head></head><body><table><tr>\n"
                + "<td>a\n"
                + "<script>scriptcontent </script>"
                + "<noscript>noscriptcontent </noscript>"
                + "<noframes>noframecontent </noframes>"
                + "<rp>rpcontent </rp>"
                + "<a>fefefe</a>"
                + "<ul>\n"
                + "<li>Coffee</li>\n"
                + "<li>Milk</li>\n"
                + "</ul></td>"
                + "<td>b</td>"
                + "</tr></table></body></html>";

        org.jsoup.nodes.Document doc = Jsoup.parse(html, "UTF-8");
        output.println("=====================html==================");
        output.println(doc.toString());
        output.println("=====================content==================");
        String content = HtmlContentExtractor.extractFromContent(html);
        output.println(content);
        output.println("=====================list==================");
        for (Element tr : doc.getElementsByTag("tr")) {
            for (Element td : tr.children()) {
                if (td.tagName().equalsIgnoreCase("td")) {
                    output.println(CandidateListHtmlExtractor.getHeadingText(td));
                }
            }
        }

        output.println("=====================travel==================");
        StringBuilder textBuilder = new StringBuilder();
        travel(doc, textBuilder);
        output.println(textBuilder.toString());

    }

    private void travel(Node node, StringBuilder text) {
        if (node instanceof TextNode) {
            TextNode textNode = (TextNode) node;
            text.append("@text:" + textNode.getWholeText());
        }

        if (node instanceof Element) {
            Element elementNode = (Element) node;
            text.append("@" + elementNode.tagName());
        }

        for (Node node2 : node.childNodes()) {
            travel(node2, text);
        }

    }

    private void testCandidateListHtmlExtractor(Parameters p, PrintStream output) throws IOException, Exception {
        Retrieval retrieval = RetrievalFactory.instance(p);

        String docName = "clueweb09-en0000-03-33663";
        Document document = retrieval.getDocument(docName, new Document.DocumentComponents(true, true, true));
        Query q = new Query("1", "test");
        //List<CandidateList> clists = new CandidateListHtmlExtractor().extract(document, q);

    }

    private void testHtmlContentExtractor(Parameters p, PrintStream output) throws Exception {
        String htmlFileName = "test.html";
//        Retrieval retrieval = RetrievalFactory.instance(p);
//        String docName = "clueweb09-en0000-03-33030";
//        Document document = retrieval.getDocument(docName, new Document.DocumentComponents(true, true, true));
//        BufferedWriter writer = Utility.getWriter("test.html");
//        writer.write(document.text);
//        writer.close();

        BufferedWriter writer = Utility.getWriter("test.txt");
        org.jsoup.nodes.Document doc = Jsoup.parse(new File(htmlFileName), "UTF-8");
        String html = Utility.readFileToString(new File(htmlFileName));
        String content = HtmlContentExtractor.extractFromContent(html);
        //writer.write(doc.text());
        writer.write(content);
        writer.close();
    }

    private void testCandidateListTextExtractor(Parameters p, PrintStream output) throws Exception {
//        String senText = "Mom's and dad's computers are updated.";
//        String treeText = "(ROOT (S (NP (NP (NP (NN Mom) (POS 's)) (CC and) (NP (NN dad) (POS 's))) (NP (NNS computers))) (VP (VBP are) (VP (VBN updated))) (. .)))";
//        String beginText = "0\t3\t6\t10\t13\t16\t26\t30\t37";
//        String endText = "3\t5\t9\t13\t15\t25\t29\t37\t38";
//
//        ParseTree tree = new ParseTree(senText, treeText, beginText, endText);
//
//        tree.printTree();

        CandidateListTextExtractor extractor = new CandidateListTextExtractor();
        String content = Utility.readFileToString(new File("test.parse"));
        List<CandidateList> clists = extractor.extract(new edu.umass.ciir.fws.crawl.Document(), new Query("a", "b"), content);
        for (CandidateList clist : clists) {
            output.println(clist);
        }

    }

}
