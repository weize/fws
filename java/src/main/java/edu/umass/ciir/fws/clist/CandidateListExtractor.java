/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.types.CandidateList;
import edu.umass.ciir.fws.types.Query;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.core.eval.QuerySetResults;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.Query")
@OutputClass(className = "edu.umass.ciir.fws.types.CandidateList")
public class CandidateListExtractor extends StandardStep<Query, CandidateList> {

    HashMap<String, List<Document>> querySetDocuments;
    CandidateListHtmlExtractor cListHtmlExtractor;

    public CandidateListExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        // load ranked lists for all queries        
        loadRankedDocuments(p); 
        cListHtmlExtractor = new CandidateListHtmlExtractor();
    }

    @Override
    public void process(Query query) throws IOException {
        List<Document> documents = querySetDocuments.get(query.id);
        for(Document doc: documents) {
            List<edu.umass.ciir.fws.clist.CandidateList> lists = cListHtmlExtractor.extract(doc, query);
            for(edu.umass.ciir.fws.clist.CandidateList list : lists) {
                processor.process(list);
            }
        }
    }

    private void loadRankedDocuments(Parameters parameters) throws Exception {
        querySetDocuments = new HashMap<>();

        String file = parameters.get("rankedListFile", "");
        long topNum = parameters.get("topNum", 100);
        Retrieval retrieval = RetrievalFactory.instance(parameters);
        querySetDocuments = Document.loadQuerySetDocuments(file, topNum, retrieval);
        retrieval.close();
    }
}
