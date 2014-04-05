/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.types.CandidateList;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.CandidateList", order = {"+qid", "+docRank", "+listType", "+itemList"})
public class CandidateListWriter implements Processor<CandidateList> {

    String clistDir;
    CandidateList last = null;
    BufferedWriter writer;

    public CandidateListWriter(TupleFlowParameters p) throws IOException {
        clistDir = p.getJSON().getString("clistDir");
    }

    @Override
    public void process(CandidateList cList) throws IOException {
        if (last == null) {
            last = cList;
            createFile(cList.qid);
        } else if (!last.qid.equals(cList.qid)) {
            writer.close();
            createFile(cList.qid);
        }

        writer.write(String.format("%s\t%d\t%s\t%s\n",
                cList.qid, cList.docRank, cList.listType, cList.itemList));
        last = cList;
    }

    @Override
    public void close() throws IOException {
        if (last != null) {
            writer.close();
        }
    }

    private void createFile(String qid) throws IOException {
        String file = clistDir + File.separator + qid + ".clist";
        writer = Utility.getWriter(file);
    }

}
