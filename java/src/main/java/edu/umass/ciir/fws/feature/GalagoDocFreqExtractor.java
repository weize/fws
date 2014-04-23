/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.types.Term;
import edu.umass.ciir.fws.types.TermCount;
import java.io.IOException;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.query.StructuredQuery;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Extract document frequency for terms from Galago index.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.Term", order = {"+term"})
@OutputClass(className = "edu.umass.ciir.fws.types.TermCount", order = {"+term"})
public class GalagoDocFreqExtractor extends StandardStep<Term, TermCount> {

    Retrieval retrieval;
    CluewebDocFreqMap clueDfs;
    boolean existsClueDfs;

    public GalagoDocFreqExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        retrieval = RetrievalFactory.instance(p);
        if (p.containsKey("clistDfOldFile")) {
            existsClueDfs = true;
            String clistDfOldFile = p.getString("clistDfOldFile");
            clueDfs = new CluewebDocFreqMap(clistDfOldFile);
        } else {
            existsClueDfs = false;
        }
    }

    @Override
    public void process(Term term) throws IOException {
        if (existsClueDfs && clueDfs.contains(term.term)) {
            long count = clueDfs.getDf(term.term);
            processor.process(new TermCount(term.term, count));
        } else {
            String query = String.format("#od:1( %s )", term.term);
            Node parsed = StructuredQuery.parse(query);
            parsed.getNodeParameters().set("queryType", "count");
            try {
                Node transformed = retrieval.transformQuery(parsed, new Parameters());
                long count = retrieval.getNodeStatistics(transformed).nodeDocumentCount;
                processor.process(new TermCount(term.term, count));

            } catch (Exception ex) {
                System.err.println("warning: failed to get docFreq for " + term.term);
            }
        }

    }

    @Override
    public void close() throws IOException {
        processor.close();
        retrieval.close();
    }

}
