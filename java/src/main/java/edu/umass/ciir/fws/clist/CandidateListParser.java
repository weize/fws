/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
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
@InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
@OutputClass(className = "edu.umass.ciir.fws.types.TfCandidateList")
public class CandidateListParser extends StandardStep<TfQuery, edu.umass.ciir.fws.types.TfCandidateList> {

    String clistDir;
    String suffix;

    public CandidateListParser(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        clistDir = p.getString("clistDir");
    }

    @Override
    public void process(TfQuery query) throws IOException {
        File clistFile = new File(Utility.getCandidateListCleanFileName(clistDir, query.id));
        List<CandidateList> clists = CandidateList.loadCandidateLists(clistFile);

        for (CandidateList clist : clists) {
            processor.process(new edu.umass.ciir.fws.types.TfCandidateList(clist.qid,
                    clist.docRank, clist.docName, clist.listType, clist.itemList));
        }
    }
}
