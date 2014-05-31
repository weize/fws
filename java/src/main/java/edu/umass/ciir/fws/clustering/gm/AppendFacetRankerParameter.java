/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.IOException;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public  class AppendFacetRankerParameter extends StandardStep<TfQueryParameters, TfQueryParameters> {

    static final String[] rankParams = new String[]{"sum", "avg"};

    public AppendFacetRankerParameter(TupleFlowParameters parameters) throws IOException {
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {

        for (String ranker : rankParams) {
            String paramsNew = Utility.parametersToString(queryParams.parameters, ranker);
            processor.process(new TfQueryParameters(queryParams.id, queryParams.text, paramsNew));
        }
    }

}
