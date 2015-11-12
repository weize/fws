/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.tool;

import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clist.CandidateListTextExtractor;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.gm.GmCoordinateAscentClusterer;
import edu.umass.ciir.fws.clustering.gm.lr.LinearRegressionModel;
import edu.umass.ciir.fws.demo.search.BingSearchEngine;
import edu.umass.ciir.fws.demo.search.BingSearchEngine.RankResult;
import edu.umass.ciir.fws.eval.ClusteringEvaluator;
import edu.umass.ciir.fws.eval.RpndcgEvaluator;
import edu.umass.ciir.fws.nlp.HtmlContentExtractor;
import edu.umass.ciir.fws.nlp.PeerPatternNLPParser;
import edu.umass.ciir.fws.query.QueryTopic;
import edu.umass.ciir.fws.query.TrecFullTopicXmlParser;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.TagTokenizer;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

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
        //testBingSearch(p);
        testCrawl(p);

        //testPrintHTML(p, output);
        //testNlp(output);
        //testTokenizer(p, output);
        //testReplace(p, output);
        //testPrintTermsInDoc(p, output);
        //testHtml(p, output);
        //testCandidateListHtmlExtractor(p, output);
        // testHtmlContentExtractor(p, output);
        //testCandidateListTextExtractor(p, output);
        // testDocument(p, output);
        //testLDA(p, output);
        //testLinearLib(p, output);
        //testGmCluster(p, output);
        //testTrecFullTopicXmlParser(p,output);
        //testQueryTopic(p, output);
        //testGMCACluster(p, output);
        //testEal(p, output);
        //testRpndcgEvaluator(p, output);
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
//            RankedDocument document = retrieval.getDocument(sd.documentName, new RankedDocument.DocumentComponents(true, true, true));
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

    private void testTokenizer(Parameters p, PrintStream output) throws IOException {
//        String text = "U.S.A <tag> and U.S. are efe©egfe the© f©fefe abbrevation_for United States of American.\n"
//                + "edu.umass.ciir.fws and edu.umass.cirr.galago are package pathes.\n"
//                + "Mom's and dad's computers are34343 234 updated.\n"
//                + "We are learning ““ “fef Español and 日本語\n"
//                + "We’ve d‘one!\n"
//                + "that`efe `` efe\n"
//                + "17.1 h\n";

        String text = "All the Features of the $9.95 Plan PLUS Ph.D.";
        output.println("================term1===========\n");
        List<String> tokens = TextProcessing.tokenize(text);
        for (String token : tokens) {
            output.println(token);
        }

        output.println("\n\n================term2===========\n");
        TagTokenizer tokenizer = new TagTokenizer();
        Document doc = new Document();
        doc.text = Utility.readFileToString(new File("test.html"));
        tokenizer.tokenize(doc);
        for (String token : doc.terms) {
            output.println(token);
        }
    }

    private void testNlp(PrintStream output) throws IOException {
        output.println("In test nlp!");
        PeerPatternNLPParser stanfordParser = new PeerPatternNLPParser();
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
                    output.println(HtmlContentExtractor.getHeadingText(td));
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
        TfQuery q = new TfQuery("1", "test");
        //List<CandidateList> clists = new CandidateListHtmlExtractor().extract(document, q);

    }

    private void testHtmlContentExtractor(Parameters p, PrintStream output) throws Exception {
        String htmlFileName = "test.html";
//        Retrieval retrieval = RetrievalFactory.instance(p);
//        String docName = "clueweb09-en0000-03-33030";
//        RankedDocument document = retrieval.getDocument(docName, new RankedDocument.DocumentComponents(true, true, true));
//        BufferedWriter writer = Utility.getWriter("test.html");
//        writer.write(document.text);
//        writer.close();

        BufferedWriter writer = Utility.getWriter("test.txt");
        String html = Utility.readFileToString(new File(htmlFileName));
        List<String> tokens = TextProcessing.tokenize(HtmlContentExtractor.extractFromContent(html));
        //writer.write(doc.text());
        writer.write(TextProcessing.join(tokens, " "));
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
        List<CandidateList> clists = extractor.extract(content, new edu.umass.ciir.fws.retrieval.RankedDocument(), new TfQuery("a", "b"));
        for (CandidateList clist : clists) {
            output.println(clist);
        }

    }

    private void testDocument(Parameters p, PrintStream output) throws Exception {
        Retrieval retrieval = RetrievalFactory.instance(p);
        String docName = "clueweb09-en0000-03-33030";
        Document doc = retrieval.getDocument(docName, new Document.DocumentComponents(true, true, false));
        File dataFile = new File("test.dat");
        Utility.copyStreamToFile(new ByteArrayInputStream(Document.serialize(doc, new Parameters())), dataFile);
        System.err.println(String.format("written in %s", dataFile.getAbsoluteFile()));
        doc = Document.deserialize(new DataInputStream(new FileInputStream(dataFile)), p, new Document.DocumentComponents(true, true, false));
        output.println(doc.toString());
    }

    private void testLDA(Parameters p, PrintStream output) {

    }

    private void testLinearLib(Parameters p, PrintStream output) throws Exception {
        int[] selectedFeatureIndice = {1, 2, 3, 4, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33};
        //LinearRegressionModel model = new LinearRegressionModel();
        LinearRegressionModel tModel = new LinearRegressionModel(selectedFeatureIndice);
        tModel.readProblem(new File("87.t.data.gz"));
//        File featureFile = new File("../exp/gm-initial/model/train.t.dat");
//        File scalerFile = new File("../exp/gm-initial/model/train.t.scaler");
//        File modelFile = new File("../exp/gm-initial/model/train.t.model");
//        File predictFile = new File("../exp/gm-initial/model/train.t.predict");
//        tModel.train(featureFile, modelFile, scalerFile);
//        tModel.predict(featureFile, modelFile, scalerFile, predictFile);
//        
//        LinearRegressionModel pModel = new LinearRegressionModel(); // all feature
//        featureFile = new File("../exp/gm-initial/model/train.p.dat.gz");
//        scalerFile = new File("../exp/gm-initial/model/train.p.scaler");
//        modelFile = new File("../exp/gm-initial/model/train.p.model");
//        predictFile = new File("../exp/gm-initial/model/train.p.predict");
//        pModel.train(featureFile, modelFile, scalerFile);
//        pModel.predict(featureFile, modelFile, scalerFile, predictFile);
    }

    private void testGmCluster(Parameters p, PrintStream output) throws IOException {

        File trainTermDataFile = new File("test/train.t.data");
        File trainTermScalerFile = new File("test/train.t.scaler");
        File trainTermModelFile = new File("test/train.t.model");
        int[] selectedFeatureIndice = {1, 2, 3, 4, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33};
        LinearRegressionModel tModel = new LinearRegressionModel(selectedFeatureIndice);
        tModel.train(trainTermDataFile, trainTermModelFile, trainTermScalerFile);

//        File trainPairDataFile = new File("test/train.p.data");
//        File trainPairScalerFile = new File("test/train.p.scaler");
//        File trainPairModelFile = new File("test/train.p.model");
//        LinearRegressionModel pModel = new LinearRegressionModel();
//        tModel.train(trainPairDataFile, trainPairModelFile, trainPairScalerFile);
        File termFeatureFile = new File("test/test.t.feature");
        File termDataFile = new File("test/test.t.data");

        System.err.println("processing " + termFeatureFile.getAbsolutePath());
        BufferedReader reader = Utility.getReader(termFeatureFile);
        BufferedWriter writer = Utility.getWriter(termDataFile);
        String line;
        while ((line = reader.readLine()) != null) {
            // format: term<tab>f1<tab>f2<tab>...
            String[] fields = line.split("\t");
            String term = fields[0];
            int label = -1;
            String data = label + "\t" + TextProcessing.join(Arrays.asList(fields).subList(1, fields.length), "\t");
            String comment = String.format("%d\t%s\t%s", label, "174", term);
            writer.write(data + "\t#" + comment);
            writer.newLine();

        }
        writer.close();
        reader.close();
        Utility.infoWritten(termDataFile);

        File termPredictFile = new File("test/test.t.precit");
        tModel.predict(termDataFile, trainTermModelFile, trainTermScalerFile, termPredictFile);
    }

    private void testTrecFullTopicXmlParser(Parameters p, PrintStream output) throws IOException {
        TrecFullTopicXmlParser parser = new TrecFullTopicXmlParser();
        List<Parameters> topics = parser.parse(new File("test.xml"));
        for (Parameters topic : topics) {
            output.println(topic.toPrettyString());
        }
    }

    private void testQueryTopic(Parameters p, PrintStream output) throws IOException {
        File input = new File(p.getString("queryJsonFile"));
        BufferedReader reader = Utility.getReader(input);
        String line;
        while ((line = reader.readLine()) != null) {
            QueryTopic q = QueryTopic.parseFromJson(line);
            output.print(q.listAsString());
        }
        reader.close();
    }

    private void testGMCACluster(Parameters p, PrintStream output) throws IOException {
        File termPredictFile = new File(p.getAsString("term"));
        File termPairPredictFile = new File(p.getAsString("pair"));
        File clusterFile = new File(p.getAsString("out"));
        GmCoordinateAscentClusterer clusterer = new GmCoordinateAscentClusterer();
        List<ScoredFacet> clusters = clusterer.cluster(termPredictFile, termPairPredictFile);
        ScoredFacet.output(clusters, clusterFile);

    }

    private void testEal(Parameters p, PrintStream output) throws IOException {
        ClusteringEvaluator clusteringEvaluator = new ClusteringEvaluator(10);
        File annotatedFacetJsonFile = new File(p.getString("facetAnnotationJson"));
        String facetFilename = p.getString("facetFilename");
        HashMap<String, FacetAnnotation> facetMap = FacetAnnotation.loadAsMap(annotatedFacetJsonFile);
        String qid = "9";
        FacetAnnotation annotator = facetMap.get(qid);
        File systemFile = new File(facetFilename);
        List<ScoredFacet> system = ScoredFacet.loadFacets(systemFile);
        double[] result = clusteringEvaluator.eval(annotator.facets, system, 10);
    }

    private void testRpndcgEvaluator(Parameters p, PrintStream output) throws IOException {
        RpndcgEvaluator RpndcgEvaluator = new RpndcgEvaluator(10);
        File annotatedFacetJsonFile = new File(p.getString("facetAnnotationJson"));
        String facetFilename = p.getString("facetFilename");
        HashMap<String, FacetAnnotation> facetMap = FacetAnnotation.loadAsMap(annotatedFacetJsonFile);
        String qid = "60";
        FacetAnnotation annotator = facetMap.get(qid);
        File systemFile = new File(facetFilename);
        List<ScoredFacet> system = ScoredFacet.loadFacets(systemFile);
        double[] result = RpndcgEvaluator.eval(annotator.facets, system, 10);
    }

    private void testBingSearch(Parameters p) throws MalformedURLException, IOException {
        BingSearchEngine bing = new BingSearchEngine(p);
        List<RankResult> results = bing.search(new TfQuery("1", "baggage allowance"), 100);
        for (RankResult res : results) {
            System.out.println(res);
        }

    }

    private void testCrawl(Parameters p) {
        URL url;
        try {
            url = new URL(p.getString("url"));
            String html = Utility.copyStreamToString(url.openStream());
            System.out.println(html);
        } catch (MalformedURLException ex) {
            Logger.getLogger(TestFn.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TestFn.class.getName()).log(Level.SEVERE, null, ex);
        }
        

    }

}
