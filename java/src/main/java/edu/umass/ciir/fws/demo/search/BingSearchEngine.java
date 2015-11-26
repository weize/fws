/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.demo.search;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class BingSearchEngine implements SearchEngine {

    String accountKeyEnc;
    static int numThreads;
    final static int bingTop = 50; // max request
    static int connectTimeout = 1000 * 3; // for donwloading webpages
    static int readTimeout = 1000 * 3;
    final static String BingURLBase = "https://api.datamarket.azure.com/Bing/Search/Web?";

    public BingSearchEngine(Parameters p) {
        setAccoutKey(p);
        numThreads = (int) p.getLong("numCrawlThread");
        connectTimeout = (int) p.getLong("connectTimeout");
        readTimeout = (int) p.getLong("readTimeout");
    }

    @Override
    public List<RankedDocument> getRankedDocuments(TfQuery query, int top) {
        Utility.info("search via Bing.com");
        List<RankResult> ranks = search(query, top);
        Utility.info("#top webpages: " + ranks.size());
        Utility.info("crawl top results");
        List<RankedDocument> docs = crawl(ranks, top);
        Utility.info("#crawled webpages: " + docs.size());
        return docs;
    }

    public List<RankResult> search(TfQuery query, int top) {
        List<RankResult> docs = new ArrayList<>();
        String queryEscaped = URLEncoder.encode(String.format("'%s'", query.text));
        HashSet<String> urlSet = new HashSet<>();
        int extra = 0; // request some extra results, since there some results could not be crawlable.
        int round = (int) (Math.ceil(top / bingTop)) + extra;
        for (int i = 0; i < round; i++) {
            try {
                int skip = bingTop * i;
                String bingUrl = String.format("%sQuery=%s&$format=JSON&$top=%d&$skip=%d", BingURLBase, queryEscaped, bingTop, skip);
                Utility.info(bingUrl);
                URL url = new URL(bingUrl);
                URLConnection urlConnection = url.openConnection();
                urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);
                Parameters response = Parameters.parseStream(urlConnection.getInputStream());
                List<Parameters> docsJson = response.getMap("d").getAsList("results", Parameters.class);
                for (Parameters doc : docsJson) {
                    RankResult result = new RankResult(doc, query.id, docs.size() + 1);
                    if (!filterOut(result, urlSet)) {
                        docs.add(result);
                        urlSet.add(result.url);
                    }
                }

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return docs;
    }

    private void setAccoutKey(Parameters p) {
        String accountKey = p.getString("bingKey");
        byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes()); // code for encoding found on stackoverflow
        accountKeyEnc = new String(accountKeyBytes);
    }

    public static class WebpageDownloader implements Runnable {

        final Queue<RankResult> ranks; // each downloader will fetch one rank each time from this shared queue
        final List<RankedDocument> docs; // each downloader will save the result into this shared list
        int top;

        public WebpageDownloader(Queue<RankResult> ranks, List<RankedDocument> docs, int top) {
            this.ranks = ranks;
            this.docs = docs;
            this.top = top;
        }

        @Override
        public void run() {

            while (true) {
                RankResult res = getNexRank();
                if (res == null) {
                    break;
                }
                try {
                    URL url = new URL(res.url);
                    URLConnection con = url.openConnection();
                    con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                    con.connect();

                    con.setConnectTimeout(connectTimeout);
                    con.setReadTimeout(readTimeout);
                    Utility.info("downloading " + res.rankUrl());
                    String html = Utility.copyStreamToString(con.getInputStream());
                    if (!filterOutHtml(html)) {
                        addDoc(new RankedDocument(res.getName(), res.rank, res.url, html));
                    } else {
                        Utility.info("filter out " + res.url + "\nContent:\n" + html);
                    }
                    
                } catch (MalformedURLException ex) {
                    Logger.getLogger(BingSearchEngine.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(BingSearchEngine.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (getDocsSize() >= top) {
                    break;
                }
            }
        }

        private boolean filterOutHtml(String html) {
            return !(html.contains("html") || html.contains("HTML"));
        }

        private RankResult getNexRank() {
            synchronized (ranks) {
                return ranks.poll();
            }
        }

        private void addDoc(RankedDocument rankedDocument) {
            synchronized (docs) {
                docs.add(rankedDocument);
            }
        }

        private int getDocsSize() {
            synchronized (docs) {
                return docs.size();
            }
        }
    }

    private List<RankedDocument> crawl(List<RankResult> ranks, int top) {
        List<RankedDocument> docs = new ArrayList<>();
        Queue<RankResult> ranksQueue = new LinkedList<>();
        ranksQueue.addAll(ranks);
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(new WebpageDownloader(ranksQueue, docs, top));
            threads[i].start();
        }

        for (int i = 0; i < numThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        docs = docs.subList(0, Math.min(docs.size(), top));
        Collections.sort(docs);

        System.out.println("crawled " + docs.size() + " webpages");
        for (RankedDocument d : docs) {
            System.out.println(d.rank + " : " + d.url);
        }

        return docs;
    }

    private boolean filterOut(RankResult result, HashSet<String> urlSet) {
        String url = result.url;
        if (urlSet.contains(url)) {
            return true;
        }
        if (url.endsWith(".ppt") || url.endsWith(".pdf") || url.endsWith(".pptx")) {
            return true;
        }
        return false;
    }

    public static class RankResult {

        String url;
        String title;
        String desc; // description
        int rank;
        String qid;

        private RankResult(Parameters doc, String qid, int rank) {
            this.title = doc.getString("Title");
            this.desc = doc.getString("Description");
            this.url = doc.getString("Url");//.replaceAll("^https", "http");
            this.qid = qid;
            this.rank = rank;
            title = title.replaceAll("\t", " ");
            desc = desc.replaceAll("\t", " ");
        }

        @Override
        public String toString() {
            return String.format("%s\t%d\t%s\t%s\t%s", qid, rank, url, title, desc);
        }
                
        public String rankUrl() {
            return String.format("%d: %s", rank, url);
        }

        public String getName() {
            return String.format("bing-%s-%d", qid, rank);
        }
    }

}
