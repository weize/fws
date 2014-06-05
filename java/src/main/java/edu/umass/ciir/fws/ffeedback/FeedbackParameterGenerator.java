/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.utility.Utility;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class FeedbackParameterGenerator {

    List<Double> oracleThs;
    List<String> oracleModels;

    public FeedbackParameterGenerator(Parameters p) {
        oracleThs = p.getAsList("oracleFeedbackImprvThresholds");
        oracleModels = p.getAsList("oracleExpansionModels");
    }

    public List<String> getParams(String type) throws IOException {

        ArrayList<String> params = new ArrayList<>();
        if (type.equals("annotator")) {
            params.add("");
        } else if (type.equals("oracle")) {
            for (String model : oracleModels) {
                for (double th : oracleThs) {
                    String param = Utility.parametersToString(model, th);
                    params.add(param);
                }
            }
        } else {
            throw new IOException("cannot recognize " + type);
        }
        return params;
    }

}
