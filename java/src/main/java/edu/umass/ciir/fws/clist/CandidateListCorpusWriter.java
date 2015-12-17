/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.retrieval.CorpusAccessor;
import edu.umass.ciir.fws.retrieval.CorpusAccessorFactory;
import edu.umass.ciir.fws.types.TfCandidateList;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.IOException;
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
public class CandidateListCorpusWriter implements Processor<TfCandidateList> {

    String fileName; // current filename
    CorpusAccessor corpusAccessor;
    String suffix;
    BufferedWriter writer;

    public CandidateListCorpusWriter(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        corpusAccessor = CorpusAccessorFactory.instance(p);
        suffix = p.getString("suffix");
        fileName = null;
    }

    @Override
    public void process(TfCandidateList clist) throws IOException {
        String newFileName = corpusAccessor.getClistFileName(clist.docName, suffix);
        if (fileName == null) {
            onNewFile(newFileName);
        } else {
            if (!newFileName.equals(fileName)) {
                writer.close();
                onNewFile(newFileName);
            }
        }

        writeClist(clist);
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    private void writeClist(TfCandidateList clist) throws IOException {
        writer.write(CandidateList.toString(clist));
        writer.newLine();
    }

    private void onNewFile(String newFileName) throws IOException {
        fileName = newFileName;
        Utility.createDirectoryForFile(fileName);
        writer = Utility.getGzipWriter(fileName);
    }

}
