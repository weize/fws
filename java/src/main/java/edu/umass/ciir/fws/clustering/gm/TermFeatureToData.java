/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
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
@InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQuery")
public class TermFeatureToData extends StandardStep<TfQuery, TfQuery> {

    String featureDir = "featureDir";
    String predictDir;
    HashMap<String, FacetAnnotation> facetMap;

    public TermFeatureToData(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        predictDir = Utility.getFileName(gmDir, "predict");
        File facetJsonFile = new File(p.getString("facetAnnotationJson"));
        facetMap = FacetAnnotation.loadAsMap(facetJsonFile);
        featureDir = p.getString("featureDir");
    }

    @Override
    public void process(TfQuery query) throws IOException {

        // load face terms
        HashSet<String> facetTermSet = new HashSet<>();
        if (facetMap.containsKey(query.id)) {
            FacetAnnotation fa = facetMap.get(query.id);
            for (AnnotatedFacet f : fa.facets) {
                if (f.isValid()) {
                    for (String term : f.terms) {
                        facetTermSet.add(term);
                    }
                }
            }
        }

        // input file
        File featureFile = new File(Utility.getTermFeatureFileName(featureDir, query.id));
        // output file
        File dataFile = new File(Utility.getGmTermDataFileName(predictDir, query.id));

        System.err.println("processing " + featureFile.getAbsolutePath());
        Utility.createDirectoryForFile(dataFile);
        BufferedReader reader = Utility.getReader(featureFile);
        BufferedWriter writer = Utility.getWriter(dataFile);
        String line;
        while ((line = reader.readLine()) != null) {
            // format: term<tab>f1<tab>f2<tab>...
            String[] fields = line.split("\t");
            String term = fields[0];
            int label = facetTermSet.contains(term) ? 1 : -1;
            String data = label + "\t" + TextProcessing.join(Arrays.asList(fields).subList(1, fields.length), "\t");
            String comment = String.format("%d\t%s\t%s", label, query.id, term);
            writer.write(data + "\t#" + comment);
            writer.newLine();

        }
        writer.close();
        reader.close();
        Utility.infoWritten(dataFile);

        processor.process(query);
    }
}
