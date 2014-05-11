/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.types.TfCandidateList;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.TreeMap;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfCandidateList", order = {"+qid", "+docRank", "+docName", "+listType", "+itemList"})
public class CandidateListDocFreqWriter implements Processor<TfCandidateList> {

    private static final int _docDF = 0;
    private static final int _docHLDF = 1;
    private static final int _listDF = 2;
    private static final int _listHLDF = 3;
    private static final int _size = 4;

    TfCandidateList last; // previous candidate list
    TfCandidateList lastHtml; // previous candidate list that are extracted by html patterns

    // if current candidate list is html type
    boolean isHtmlType;

    String clistDfFile;
    TreeMap<String, Long[]> termDfs;
    HashSet<String> queryTermSet;
    HashSet<String> docTermSet;
    HashSet<String> queryTermHtmlSet;
    HashSet<String> docTermHtmlSet;
    Long[] totals; // total counts for queries, documents and lists

    public CandidateListDocFreqWriter(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        clistDfFile = p.getString("clistDfFile");
        termDfs = new TreeMap<>();
        last = null;
        lastHtml = null;
        totals = getZeros();
        queryTermSet = new HashSet<>();
        docTermSet = new HashSet<>();
        queryTermHtmlSet = new HashSet<>();
        docTermHtmlSet = new HashSet<>();

    }

    /**
     * Candidate list can be textual or html, and thus need to handle df for
     * html candidate lists with another set of hash set.
     *
     * @param clist
     * @throws IOException
     */
    @Override
    public void process(TfCandidateList clist) throws IOException {
        isHtmlType = CandidateList.isHtmlCandidateList(clist);

        // new doc
        if (last == null || !last.docName.equals(clist.docName)) {
            docTermSet.clear();
            totals[_docDF]++;
        }
        last = clist;
        totals[_listDF]++;

        if (isHtmlType) {
            // new doc for html candidate list
            if (lastHtml == null || !lastHtml.docName.equals(clist.docName)) {
                docTermHtmlSet.clear();
                totals[_docHLDF]++;
            }
            lastHtml = clist;
            totals[_listHLDF]++;
        }

        for (String item : CandidateList.splitItemList(clist.itemList)) {
            Long[] counts = termDfs.containsKey(item) ? termDfs.get(item) : getZeros();

            if (!docTermSet.contains(item)) {
                counts[_docDF]++;
                docTermSet.add(item);
            }
            counts[_listDF]++;

            // for html type
            if (isHtmlType) {
                if (!docTermHtmlSet.contains(item)) {
                    counts[_docHLDF]++;
                    docTermHtmlSet.add(item);
                }
                counts[_listHLDF]++;
            }

            termDfs.put(item, counts);
        }
    }

    @Override
    public void close() throws IOException {
        BufferedWriter writer = Utility.getGzipWriter(this.clistDfFile);
        writer.write("#term\tdocDF\tdocDFHtml\tlistDF\tlistDFHtml\n");
        writer.write("#collection\t" + TextProcessing.join(totals, "\t") + "\n");
        for (String item : termDfs.keySet()) {
            writer.write(item + "\t" + TextProcessing.join(termDfs.get(item), "\t"));
            writer.newLine();
        }
        writer.close();
        System.err.println("Written in " + clistDfFile);
    }

    private Long[] getZeros() {
        Long[] zeros = new Long[_size];
        for (int i = 0; i < _size; i++) {
            zeros[i] = new Long(0);
        }
        return zeros;
    }

}
