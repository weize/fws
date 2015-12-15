/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.qf13;

import edu.umass.ciir.fws.retrieval.QuerySetResults;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import org.lemurproject.galago.core.eval.QueryResults;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Type;

/**
 * Copy(and convert) qf13 documents into current project direcoty
 * @author wkong
 */
public class CopyDocs extends AppFunction {

    @Override
    public String getName() {
        return "qf13-cp-docs";
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        String qfDocDir = "/usr/dan/users7/wkong/work2/qAspect/experiment/webpage";
        String crawledDocFile = "/usr/dan/users7/wkong/work2/qAspect/experiment/webpage/bing-ranks1_5.crawled";
        String docDir = p.getString("docDir");
        String rankedListFile = p.getString("rankedListFile");
        int topNum = 10000;
        QuerySetResults querySetResults = new QuerySetResults(rankedListFile, topNum);
        HashMap<String, RankedDocument> crawledDocMap = loadCrawledDocMap(crawledDocFile);

        for (String qid : querySetResults.getQueryIterator()) {
            QueryResults results = querySetResults.get(qid);
            Utility.infoProcessingQuery(qid);
            String dir = String.format("%s/%s", docDir, qid);
            Utility.createDirectory(dir);
            
            for (ScoredDocument d : results.getIterator()) {
                Utility.infoProcessing(d);
                File from = new File(String.format("%s/%s/%s.html", qfDocDir, qid, d.documentName));
                File to = new File(Utility.getDocHtmlFileName(docDir, qid, d.documentName));
                String text = Utility.copyStreamToString(new FileInputStream(from));
                Utility.copyStringToFile(text, to);
                Utility.infoWritten(to);
                
                Document doc = new Document();
                doc.identifier = (Integer.parseInt(qid)-1)*10000 + d.rank;
                doc.name = d.documentName;
                doc.text = text;
                RankedDocument rd = crawledDocMap.get(doc.name);
                doc.metadata.put("url", rd.url);
                doc.metadata.put("snippet", rd.name); // name was used for store snippet
                doc.metadata.put("title", rd.title); // name was used for store snippet

                File dataFile = new File(Utility.getDocDataFileName(docDir, qid, doc.name));
                Utility.copyStreamToFile(new ByteArrayInputStream(Document.serialize(doc, new Parameters())), dataFile);
                Utility.infoWritten(dataFile);
            }
        }

    }

    private HashMap<String, RankedDocument> loadCrawledDocMap(String crawledDocFile) throws IOException {
        HashMap<String, RankedDocument> map = new HashMap<>();
        BufferedReader reader = Utility.getReader(crawledDocFile);
        String line;
        while ((line = reader.readLine()) != null) {
            //System.err.println(line);
            String[] elems = line.split("\t", 5);
            String qid = elems[0];
            String rank = elems[1];
            String url = elems[2];
            String title = elems[3];
            String snippet = elems[4];
            RankedDocument rd = new RankedDocument();
            String name = String.format("bing-%s-%s", qid, rank);
            rd.name = snippet; // use name field for snippet !!! 
            rd.url = url;
            rd.title = title;
            map.put(name, rd);
        }
        reader.close();
        return map;
    }

}
