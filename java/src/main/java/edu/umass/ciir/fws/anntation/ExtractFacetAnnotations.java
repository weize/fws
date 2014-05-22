/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class ExtractFacetAnnotations extends AppFunction {

    @Override
    public String getName() {
        return "extract-facet-annotations";
    }

    @Override
    public String getHelpString() {
        return "fws extract-facet-annotations config.json";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        File jsonFile = new File(p.getString("facetAnnotationJson"));
        File outfile = new File(p.getString("facetAnnotationText"));
        List<FacetAnnotation> anootations = FacetAnnotation.load(jsonFile);
        BufferedWriter writer = Utility.getWriter(outfile);
        writer.write("#anntatorID\tqid\tfid\tdescription\trating\tterms\n");
        for (FacetAnnotation facetAnnotation : anootations) {
            writer.write(facetAnnotation.listAsString());
        }
        writer.close();
        Utility.infoWritten(outfile);
    }

}
