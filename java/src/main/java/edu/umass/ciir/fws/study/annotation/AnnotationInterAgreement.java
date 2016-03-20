/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.study.annotation;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.eval.PrfEqualEvaluator;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * select candidate list types
 *
 * @author wkong
 */
public class AnnotationInterAgreement extends AppFunction {

    int numTopFacets = 10;

    @Override
    public String getName() {
        return "annotation-inter-agreement";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        String file1 = p.getString("annotation1");
        String file2 = p.getString("annotation2");
        this.numTopFacets = (int)p.get("top", 10);
        PrfEqualEvaluator evaluator = new PrfEqualEvaluator();

        double[] res1 = eval(evaluator, file1, file2);
        double[] res2 = eval(evaluator, file2, file1);
        System.out.println(TextProcessing.join(res1, "\t"));
        System.out.println(TextProcessing.join(res2, "\t"));

    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private List<AnnotatedFacet> readAsAnnotatedFacet(String file) throws IOException {

        ArrayList<AnnotatedFacet> facets = new ArrayList<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        int id = 0;
        while ((line = reader.readLine()) != null) {
            String aid = "test";
            String qid = "test";
            String fid =""+ (++ id);
            int rating = 2;
            String des = "test";
            String termsStr = line;

            AnnotatedFacet f = new AnnotatedFacet(rating, fid, des);
            for (String term : termsStr.split("\\|")) {
                f.addTerm(term);
            }

            facets.add(f);
        }
        reader.close();

        return facets;
    }

    private List<ScoredFacet> readAsScoredFacet(String file) throws IOException {
        ArrayList<ScoredFacet> facets = new ArrayList<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        double oscore = 1;
        while ((line = reader.readLine()) != null) {
            double score = oscore;
            oscore -= 0.01;
            String scoredItemList = line;
            ArrayList<ScoredItem> items = new ArrayList<>();
            for (String scoredItemStr : scoredItemList.split("\\|")) {
                ScoredItem scoredItem = new ScoredItem(scoredItemStr, 0);
                items.add(scoredItem);
            }
            facets.add(new ScoredFacet(items, score));

        }
        reader.close();

        return facets;
    }

    private double[] eval(PrfEqualEvaluator evaluator, String annotation, String system) throws IOException {
        List<AnnotatedFacet> afacets = readAsAnnotatedFacet(annotation);
        List<ScoredFacet> sfacets = readAsScoredFacet(system);
        return evaluator.eval(afacets, sfacets, numTopFacets);
    }

}
