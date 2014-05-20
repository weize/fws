/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintStream;
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
        BufferedReader reader = Utility.getReader(jsonFile);
        BufferedWriter writer = Utility.getWriter(outfile);
        String line;
        while ((line = reader.readLine()) != null) {
            FacetAnnotation facetAnnotation = FacetAnnotation.parseFromJson(line);
            if (facetAnnotation != null) {
                writer.write(facetAnnotation.listAsString());
                writer.newLine();
            }
        }
        reader.close();
        writer.close();
        Utility.infoWritten(outfile);
    }

}
