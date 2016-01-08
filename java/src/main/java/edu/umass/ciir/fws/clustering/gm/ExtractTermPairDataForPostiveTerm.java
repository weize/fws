/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.feature.TermPairFeatureExtractor;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * data for train pair model
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQuery")
public class ExtractTermPairDataForPostiveTerm extends StandardStep<TfQuery, TfQuery> {

    String predictDir;
    HashMap<String, FacetAnnotation> facetMap;
    TermPairFeatureExtractor pfExtractor;

    public ExtractTermPairDataForPostiveTerm(TupleFlowParameters parameters) throws IOException, Exception {
        Parameters p = parameters.getJSON();
        String gmDir = Utility.getFileName(p.getString("facetRunDir"), "gm");
        predictDir = Utility.getFileName(gmDir, "predict");
        File facetTextFile = new File(p.getString("facetAnnotationText"));
        facetMap = FacetAnnotation.loadAsMapFromTextFile(facetTextFile);
        pfExtractor = new TermPairFeatureExtractor(p);
    }

    @Override
    public void process(TfQuery query) throws IOException {

        // load facet terms
        File termFeatureFile = new File(Utility.getGmTermDataFileName(predictDir, query.id));
        HashSet<String> termsInCandidateLists = loadTerms(termFeatureFile);

        ArrayList<ScoredItem> items = new ArrayList<>();
        if (facetMap.containsKey(query.id)) {
            FacetAnnotation fa = facetMap.get(query.id);
            for (AnnotatedFacet f : fa.facets) {
                if (f.isValid()) {
                    for (String term : f.terms) {
                        // need to make sure this term also appear in candidate lists
                        if (termsInCandidateLists.contains(term)) {
                            items.add(new ScoredItem(term, 0));
                        }
                    }
                }
            }
        }

        // output file
        File dataFile = new File(Utility.getGmPtTermPairDataFileName(predictDir, query.id));

        pfExtractor.extract(items, query.id);
        pfExtractor.output(dataFile, facetMap.get(query.id));
        Utility.infoWritten(dataFile);

        processor.process(query);
    }

    private HashSet<String> loadTerms(File termFeatureFile) throws IOException {
        HashSet<String> terms = new HashSet<>();
        BufferedReader reader = Utility.getReader(termFeatureFile);
        String line;
        while ((line = reader.readLine()) != null) {
            // label feature1 feature2 ... feature_n #label qid term
            String[] dataComment = line.split("#", 2);
            String comment = dataComment[1].trim();
            String term = comment.split("\t")[2];
            terms.add(term);

        }
        reader.close();
        return terms;
    }
}
