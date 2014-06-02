/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class FacetModelParamGenerator {

    Parameters p;
    final static String[] gmRankers = new String[]{"sum", "avg"};

    public FacetModelParamGenerator(Parameters p) {
        this.p = p;
    }

    public List<String> getParams(String model) throws IOException {
        List<Long> facetTuneMetricIndices = p.getAsList("facetTuneMetricIndices");

        ArrayList<String> params = new ArrayList<>();
        if (model.equals("plsa") || model.equals("lda") || model.equals("qd")) {
            for (long idx : facetTuneMetricIndices) {
                params.add(String.valueOf(idx));
            }
        } else if (model.equals("gmj")) {
            for (String ranker : gmRankers) {
                params.add(ranker);
            }
        } else if (model.equals("gmi")) {
            for (long idx : facetTuneMetricIndices) {
                for (String ranker : gmRankers) {
                    String param = Utility.parametersToString(ranker, idx);
                    params.add(param);
                }
            }

        } else {
            throw new IOException("cannot recognize " + model);
        }
        return params;
    }

}
