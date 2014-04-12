/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.crawl.Document;
import edu.umass.ciir.fws.crawl.QuerySetDocuments;
import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Tupleflow parser that reads candidate lists from file for each query, and
 * also contains some utility functions for candidate lists.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.Query")
@OutputClass(className = "edu.umass.ciir.fws.types.CandidateList")
public class CandidateListParser extends StandardStep<Query, edu.umass.ciir.fws.types.CandidateList> {

    String clistDir;
    String suffix;

    public CandidateListParser(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        clistDir = p.getString("clistDir");
        suffix = p.getString("suffix");
    }

    @Override
    public void process(Query query) throws IOException {
        String fileName = Utility.getCandidateListFileName(clistDir, query.id, suffix);
        String[] lines = Utility.readFileToString(new File(fileName)).split("\n");
        for (String line : lines) {
            String[] fields = line.split("\t");
            String qid = fields[0];
            long docRank = Long.parseLong(fields[1]);
            String listType = fields[2];
            String itemList = fields[3];
            processor.process(new edu.umass.ciir.fws.types.CandidateList(qid, docRank, listType, itemList));
        }
    }

    /**
     * Load candidate lists from a file.
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static List<CandidateList> loadCandidateList(String fileName) throws IOException {
        ArrayList<CandidateList> clist = new ArrayList<>();
        String[] lines = Utility.readFileToString(new File(fileName)).split("\n");
        for (String line : lines) {
            String[] fields = line.split("\t");
            String qid = fields[0];
            long docRank = Long.parseLong(fields[1]);
            String listType = fields[2];
            String itemList = fields[3];
            String[] items = splitItemList(itemList);
            clist.add(new CandidateList(qid, docRank, listType, itemList, items));
        }
        return clist;
    }

    public static String[] splitItemList(String itemList) {
        return itemList.split("\\|");
    }

    public static String joinItemList(String[] items) {
        return Utility.join(items, "|");
    }

    public static String joinItemList(List<String> items) {
        return Utility.join(items, "|");
    }

    /**
     * if the candidate list is extracted based on html patterns.
     * @param candidateList
     * @return 
     */
    public static boolean isHtmlCandidateList(edu.umass.ciir.fws.types.CandidateList candidateList) {
        return !candidateList.listType.equals(CandidateListTextExtractor.type);
    }
}
