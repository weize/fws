/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.crawl.QuerySetDocuments;
import edu.umass.ciir.fws.crawl.Document;
import edu.umass.ciir.fws.types.CandidateList;
import edu.umass.ciir.fws.types.Query;
import java.io.IOException;
import java.util.List;
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

    QuerySetDocuments querySetDocuments;
    CandidateListHtmlExtractor cListHtmlExtractor;

    public CandidateListExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        querySetDocuments = new QuerySetDocuments(p);
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
}
