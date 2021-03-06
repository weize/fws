/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.types.TfTerm;
import edu.umass.ciir.fws.types.TfTermCount;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
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
@InputClass(className = "edu.umass.ciir.fws.types.TfTerm", order = {"+term"})
@OutputClass(className = "edu.umass.ciir.fws.types.TfTermCount", order = {"+term"})
public class GalagoDocFreqExtractor extends StandardStep<TfTerm, TfTermCount> {

    Retrieval retrieval;
    CluewebDocFreqMap clueDfs;
    boolean existsClueDfs;

    public GalagoDocFreqExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        retrieval = RetrievalFactory.instance(p);
        double clueCdf = p.getLong("clueCdf");
        if (p.containsKey("clueDfOldFile")) {
            existsClueDfs = true;
            File clueDfOldFile = new File(p.getString("clueDfOldFile"));
            clueDfs = new CluewebDocFreqMap(clueDfOldFile, clueCdf);
            System.err.println("Load old clue dfs from " + clueDfOldFile.getAbsolutePath());
        } else {
            existsClueDfs = false;
        }
    }

    @Override
    public void process(TfTerm term) throws IOException {
        Utility.infoProcessing(term);
        if (existsClueDfs && clueDfs.contains(term.term)) {
            long count = clueDfs.getDf(term.term);
            processor.process(new TfTermCount(term.term, count));
        } else {
            String query = String.format("#od:1( %s )", term.term);
            Node parsed = StructuredQuery.parse(query);
            parsed.getNodeParameters().set("queryType", "count");
            try {
                Node transformed = retrieval.transformQuery(parsed, new Parameters());
                long count = retrieval.getNodeStatistics(transformed).nodeDocumentCount;
                processor.process(new TfTermCount(term.term, count));

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
