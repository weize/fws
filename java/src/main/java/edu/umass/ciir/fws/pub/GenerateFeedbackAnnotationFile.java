/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.pub;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.query.QuerySubtopic;
import edu.umass.ciir.fws.query.QueryTopic;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class GenerateFeedbackAnnotationFile extends AppFunction {

    @Override
    public String getName() {
        return "generate-facet-annotation";
    }

    @Override
    public String getHelpString() {
        return "fws generate-facet-annotation --output=<output>";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        File queryJsonFile = new File(p.getString("queryJsonFile"));
        HashMap<String, QueryTopic> queryMap = QueryTopic.loadQueryFullTopicsAsMap(queryJsonFile);
        File jsonFile = new File(p.getString("facetAnnotationJson"));
        File outfile = new File(p.getString("output"));
        List<FacetAnnotation> anootations = FacetAnnotation.load(jsonFile);
        Collections.sort(anootations, new Comparator<FacetAnnotation>() {

            @Override
            public int compare(FacetAnnotation o1, FacetAnnotation o2) {
                return Integer.parseInt(o1.qid) - Integer.parseInt(o2.qid);
            }
        });
        
        Parameters data = new Parameters();
        List<Parameters> facetsList = new ArrayList<>();
        for(FacetAnnotation fa : anootations) {
            QueryTopic qt = queryMap.get(fa.qid);
            Parameters faParameters = fa.toParameters();
            faParameters.put("query", qt.query);
            facetsList.add(faParameters);
        }
        
        data.put("facet-annotations", facetsList);
        BufferedWriter writer = Utility.getWriter(outfile);
        writer.write(data.toPrettyString());
        writer.newLine();
        writer.close();
        Utility.infoWritten(outfile);
    }
}
