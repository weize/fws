/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.ModelParameters;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiClusterParameters;
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
    GmiParameterSettings gmiSettings;

    public AppendFacetRankerParameter(TupleFlowParameters parameters) throws IOException {
        gmiSettings = new GmiParameterSettings(parameters.getJSON());
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        GmiClusterParameters clusterParams = new GmiClusterParameters(queryParams.parameters);
        
        for (ModelParameters facetParams : gmiSettings.appendFacetSettings(clusterParams)) {
            processor.process(new TfQueryParameters(queryParams.id, queryParams.text, facetParams.toString()));
        }
    }

}
