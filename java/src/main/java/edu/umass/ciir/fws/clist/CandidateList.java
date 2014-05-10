/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.types.TfCandidateList;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A more richer representation for a candidate list than
 * edu.umass.ciir.fws.types.CandidateList generated by Tupleflow-type-builder.
 *
 * @author wkong
 */
public class CandidateList {

    public String qid;
    public long docRank;
    public String docName;
    public String listType;
    public String itemList;
    public String[] items; // candidate list items
    public final static int MAX_TERM_SIZE = 10; // maxium number of word in an candidate item/a facet term

    public CandidateList() {

    }

    public CandidateList(String qid, long docRank, String docName, String listType, List<String> items) {
        this.qid = qid;
        this.docRank = docRank;
        this.docName = docName;
        this.listType = listType;
        this.items = items.toArray(new String[0]);
        this.itemList = joinItemList(items);
    }

    public CandidateList(String qid, long docRank, String docName, String listType, String itemList) {
        this.qid = qid;
        this.docRank = docRank;
        this.docName = docName;
        this.listType = listType;
        this.items = splitItemList(itemList);
        this.itemList = itemList;
    }

    public boolean valid() {
        return items.length > 1;
    }

    /**
     * if the candidate list is html type (extracted based on html patterns).
     *
     * @return
     */
    public boolean isHtmlType() {
        return !isTextType();
    }

    public boolean isTextType() {
        return listType.equals(CandidateListTextExtractor.type);
    }

    @Override
    public String toString() {
        return String.format("%s\t%d\t%s\t%s\t%s", qid, docRank, docName, listType, itemList);
    }

    public static String toString(TfCandidateList clist) {
        return String.format("%s\t%d\t%s\t%s\t%s", clist.qid, clist.docRank, clist.docName, clist.listType, clist.itemList);
    }

    public static void output(List<CandidateList> clists, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (CandidateList clist : clists) {
            writer.write(clist.toString() + "\n");
        }
        writer.close();
    }

    public static List<CandidateList> loadCandidateLists(File clistFile) throws IOException {
        ArrayList<CandidateList> clists = new ArrayList<>();
        BufferedReader reader = Utility.getReader(clistFile);
        while (true) {
            CandidateList clist = readOne(reader);
            if (clist == null) {
                break;
            } else {
                clists.add(clist);
            }
        }
        reader.close();
        return clists;
    }

    public static List<CandidateList> loadCandidateLists(File clistFile, long topNum) throws IOException {
        ArrayList<CandidateList> clists = new ArrayList<>();
        BufferedReader reader = Utility.getReader(clistFile);
        while (true) {
            CandidateList clist = readOne(reader);
            if (clist == null) {
                break;
            } else {
                if (clist.docRank <= topNum) {
                    clists.add(clist);
                }
            }
        }
        reader.close();
        return clists;
    }

    /**
     * Read one candidate list from reader.
     *
     * @param reader
     * @return
     * @throws IOException
     */
    public static CandidateList readOne(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return null;
        } else {
            String[] fields = line.split("\t");
            String qid = fields[0];
            long docRank = Long.parseLong(fields[1]);
            String docName = fields[2];
            String listType = fields[3];
            String itemList = fields[4];
            return new CandidateList(qid, docRank, docName, listType, itemList);
        }
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
     *
     * @param candidateList
     * @return
     */
    public static boolean isHtmlCandidateList(edu.umass.ciir.fws.types.TfCandidateList candidateList) {
        return !candidateList.listType.equals(CandidateListTextExtractor.type);
    }

    TfCandidateList toTfCandidateList() {
        return new TfCandidateList(this.qid, this.docRank, this.docName, this.listType, this.itemList);
    }
}
