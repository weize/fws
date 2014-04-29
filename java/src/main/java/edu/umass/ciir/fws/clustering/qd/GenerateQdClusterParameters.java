/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

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
public class GenerateQdClusterParameters extends StandardStep<Query, QueryParameters> {

    List<Double> distanceMaxs;
    List<Double> websiteCountMins;

    
    public GenerateQdClusterParameters(TupleFlowParameters parameters) {
         Parameters p = parameters.getJSON();
         distanceMaxs = p.getList("qdDistanceMaxs");
         websiteCountMins = p.getList("qdWebsiteCountMins");
        
    }
    
    @Override
    public void process(Query query) throws IOException {
        for (double distanceMax : distanceMaxs) {
            for (double websiteCountMin : websiteCountMins) {
                String parameters = Utility.parametersToString(distanceMax, websiteCountMin);
                processor.process(new QueryParameters(query.id, query.text, parameters));
            }
        }

    }

}
