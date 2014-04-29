/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.plsa;

import edu.umass.ciir.fws.clustering.qd.*;
import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.types.QueryParameters;
import edu.umass.ciir.fws.utility.Utility;
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
@OutputClass(className = "edu.umass.ciir.fws.types.QueryParameters")
public class GeneratePlsaFacetParameters extends StandardStep<Query, QueryParameters> {

    List<Long> plsaTopicNums;
    List<Long> plsaTermNums;

    public GeneratePlsaFacetParameters(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        plsaTopicNums = p.getList("plsaTopicNums");
        plsaTermNums = p.getList("plsaTopicNums");
    }

    @Override
    public void process(Query query) throws IOException {
        for (long plsaTopicNum : plsaTopicNums) {
            for (long plsaTermNum : plsaTermNums) {
                String parameters = Utility.parametersToString(plsaTopicNum, plsaTermNum);
                processor.process(new QueryParameters(query.id, query.text, parameters));
            }
        }

    }

}
