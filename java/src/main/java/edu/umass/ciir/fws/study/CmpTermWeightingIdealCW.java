/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.study;

import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.eval.PrfNewEvaluator;
import edu.umass.ciir.fws.eval.PrfNewEvaluator.TermWeighting;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Compare ideal cumulative weight for different term weighting. The hypothesis
 * is fe and fe have more skewed distribution of ideal CW over ranks.
 *
 * @author wkong
 */
public class CmpTermWeightingIdealCW extends AppFunction {

    static int topFacets = 10;
    String outputFile = "../exp/study/term-weighting/avg-cumul-ideal-weight";

    @Override
    public String getName() {
        return "study-cmp-term-weighting";
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        String annotatedFacetTextFile = p.getString("facetAnnotationText");

        //[rank][query][term-weighting]
        ArrayList<ArrayList<double[]>> weights = new ArrayList<>();
        HashMap<String, FacetAnnotation> facetMap = FacetAnnotation.loadAsMapFromTextFile(new File(annotatedFacetTextFile));

        for (String qid : facetMap.keySet()) {
            FacetAnnotation annotation = facetMap.get(qid);
            PrfNewEvaluator evaluator = new PrfNewEvaluator();
            evaluator.loadAnnotatorFacets(annotation.facets, topFacets);
            evaluator.loadItemWeightMap();
            evaluator.cumulateTermWeights();

            double[][] idealCW = evaluator.idealTermCW;
            for (int i = 0; i < idealCW.length; i++) {
                if (weights.size() <= i) {
                    weights.add(new ArrayList<double[]>());
                }
                // add cumulative weights of current query for 4 different term weighting
                // to the list for rank i
                // the list will collect different query cases at this rank
                weights.get(i).add(idealCW[i]);
            }
        }

        outputAvg(weights);
    }

    private void outputAvg(ArrayList<ArrayList<double[]>> weights) throws IOException {
        Utility.createDirectory(outputFile);
        BufferedWriter writer = Utility.getWriter(outputFile);
        writer.write("#rank\tTermEqual\tTermRating\tFacetEqual\tFacetRating\n");
        for (int i = 0; i < weights.size(); i++) {
            List<double[]> queries = weights.get(i);
            // avg
            double[] avg = new double[TermWeighting.size()];
            for (double[] cur : queries) {
                for (int j = 0; j < TermWeighting.size(); j++) {
                    avg[i] += cur[j];
                }
            }

            Utility.avg(avg, queries.size());
            writer.write(String.format("%d\t%s\n", i+1, TextProcessing.join(avg, "\t")));
        }
        writer.close();
    }

}
