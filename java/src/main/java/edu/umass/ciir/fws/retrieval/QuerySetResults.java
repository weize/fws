/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.retrieval;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.lemurproject.galago.core.eval.QueryResults;
import org.lemurproject.galago.core.eval.stat.NaturalOrderComparator;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * This is copied from org.lemurproject.galago.core.eval.QuerySetResults. Some
 *
 * @author wkong
 */
public class QuerySetResults {

    private String name;
    private Map<String, QueryResults> querySetResults = new TreeMap<>(new NaturalOrderComparator());

    public QuerySetResults(Map<String, List<ScoredDocument>> results) {
        name = "results";
        for (String query : results.keySet()) {
            List<ScoredDocument> rankedList = results.get(query);
            check(rankedList);
            Collections.sort(rankedList, new ScoredDocument.RankComparator());
            querySetResults.put(query, new QueryResults(query, rankedList));
        }
    }

    public QuerySetResults(String filename, long topNum) throws IOException {
        name = filename;
        loadRanking(filename, topNum);
    }

    public Iterable<String> getQueryIterator() {
        return querySetResults.keySet();
    }

    public QueryResults get(String query) {
        return querySetResults.get(query);
    }

    public String getName() {
        return name;
    }

    /**
     * Reads in a TREC ranking file.
     *
     * @param filename
     * @param topNum
     * @throws IOException
     */
    private void loadRanking(String filename, long topNum) throws IOException {
        // open file
        BufferedReader in = new BufferedReader(new FileReader(filename), 256 * 1024);
        String line = null;
        TreeMap<String, List<ScoredDocument>> ranking = new TreeMap<>();

        while ((line = in.readLine()) != null) {
            int[] splits = splits(line, 6);

            // 1 Q0 WSJ880711-0086 39 -3.05948 Exp
            String queryNumber = line.substring(splits[0], splits[1]);
            String unused = line.substring(splits[2], splits[3]);
            String docno = line.substring(splits[4], splits[5]);
            int rank = Integer.parseInt(line.substring(splits[6], splits[7]));
            String score = line.substring(splits[8], splits[9]);
            String runtag = line.substring(splits[10]);

            if (rank <= topNum) {
                ScoredDocument document = new ScoredDocument(docno, rank, Double.parseDouble(score));
                if (!ranking.containsKey(queryNumber)) {
                    ranking.put(queryNumber, new ArrayList<ScoredDocument>());
                }
                ranking.get(queryNumber).add(document);
            }
        }

        // ensure sorted order by rank
        for (String query : ranking.keySet()) {
            List<ScoredDocument> documents = ranking.get(query);
            Collections.sort(documents, new ScoredDocument.RankComparator());
            querySetResults.put(query, new QueryResults(query, documents));
        }

        in.close();
    }

    /**
     * Given a list of queries this function ensures that all queries exist in
     * the query set - avoids problems where no documents are returned
     */
    public void ensureQuerySet(List<Parameters> queries) {
        for (Parameters query : queries) {
            if (query.isString("number")) {
                String num = query.getString("number");
                if (!querySetResults.containsKey(num)) {
                    querySetResults.put(num, new QueryResults(num, new ArrayList<ScoredDocument>()));
                }
            }
        }
    }

    /**
     * Finds characters to split a line of a ranking file or a judgment file
     */
    private int[] splits(String s, int columns) {
        int[] result = new int[2 * columns];
        boolean lastWs = true;
        int column = 0;
        result[0] = 0;

        for (int i = 0; i < s.length() && column < columns; i++) {
            char c = s.charAt(i);
            boolean isWs = (c == ' ') || (c == '\t');

            if (!isWs && lastWs) {
                result[2 * column] = i;
            } else if (isWs && !lastWs) {
                result[2 * column + 1] = i;
                column++;
            }

            lastWs = isWs;
        }

        return result;
    }

    private void check(List<ScoredDocument> rankedList) {
        for (ScoredDocument sdoc : rankedList) {
            assert (sdoc.rank != 0) : "Ranked list contains a document with zero rank. Ranked lists must start from 1.";
        }
    }
}
