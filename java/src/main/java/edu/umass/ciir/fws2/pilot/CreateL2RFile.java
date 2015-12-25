/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws2.pilot;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.eval.CombinedEvaluator;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class CreateL2RFile extends AppFunction {

    @Override
    public String getName() {
        return "pilot-create-l2r-file";
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        // load annotations
        String facetAnnotationFileName = "annotation/qrel";
        HashMap<String, FacetAnnotation> map = loadFacetAnnotation(facetAnnotationFileName);
        String queryFile = p.getString("query");
        TfQuery[] queries = QueryFileParser.loadQueryList(queryFile);

        double[] mean = new double[]{6.71388216303471, 1.59144364763807, 15.1535474789807, 3.2838245512129, 12.0564756651173, 2.60988534415236, 11.3843821564074, 2.46505819002564, 22.7945173707337, 5.16676143395792, 17.05690943293, 3.85266044093021, 17.5876966806531, 3.66704071242486, 16.2725210608582, 3.67348432400573, 1.68550231808038, 0.359583882438579, 1.64227591367251, 0.351377846609703, 1.58152296165051, 0.337864644942608, 3.18162024480105, 0.712540968484174, 3.04103650495429, 0.683035094734453, 2.9740002612765, 0.668813275685124, 24.7704531448593, 5.75892106503758, 69.7301442320497, 16.7475582423441, 122.996964324809, 27.3340228415792, 242.75273484093, 53.7019873049556, 3.79987273389828, 0.858621614642653};
        double[] sdev = new double[]{8.94089060439025, 0.989351225015827, 22.6981550013334, 0.758279647899651, 18.7246201227778, 0.650383303123424, 17.8085829934817, 0.62719773566621, 29.0636056845574, 1.2912554476721, 22.7626555823552, 0.990558447651515, 31.9036014003232, 2.37239809096576, 21.8514171644896, 0.976739025812738, 3.88001322304444, 0.634457861172139, 3.77172208037191, 0.620420076497564, 3.66186560609821, 0.598658424754911, 5.59222797130992, 0.850803938333124, 5.2974634364677, 0.807499987136264, 5.16945107576411, 0.788210311427892, 34.0757805008423, 3.47176168553539, 76.5942492814854, 3.14443369978166, 180.061465127712, 15.3032623621243, 336.99338421615, 13.0696861025757, 4.88104386708863, 0.108066385352276};
        for (TfQuery q : queries) {
            String qid = q.id;
            // load scored facets
            String facetFilename = Utility.getFileName("data", qid, "gmj_fix.aspect");
            List<ScoredFacet> facets = ScoredFacet.loadFacets(new File(facetFilename));

            // load facet term features;
            String ftfeatureFilename = Utility.getFileName("data", qid, "kp_feature");
            HashMap<String, List<Double>> features = loadFeatures(ftfeatureFilename);

            // load facet term features;
            String ftpFilename = Utility.getFileName("data", qid, "kp_feature.lr.res");
            HashMap<String, Double> probs = loadTermProb(ftpFilename);
            for (String t : probs.keySet()) {
                features.get(t).add(probs.get(t));
            }

            for (ScoredFacet f : facets) {
                HashSet<String> sysItems = new HashSet<>();
                for (ScoredItem item : f.items) {
                    sysItems.add(item.item);
                }
                FacetAnnotation fa = map.get(qid);
                double optimalF1 = -1;
                double optimalP = -1;
                for (AnnotatedFacet af : fa.facets) {
                    int count = 0;
                    for (String t : af.terms) {
                        if (sysItems.contains(t)) {
                            count++;
                        }
                    }
                    double precision = (double) count / sysItems.size();
                    double recall = (double) count / af.size();
                    double f1 = CombinedEvaluator.f1(precision, recall);
                    optimalF1 = Math.max(f1, optimalF1);
                    optimalP = Math.max(precision, optimalP);
                }

                //int label = (int) Math.round(optimalF1 * 5);
                int label = (int) Math.round(optimalP * 5);
                List<Double> aggFeatures = new ArrayList<>();
                String cmt = f.toFacetString();
                for (int i = 0; i < 19; i++) {
                    double sum = 0;
                    for (String t : sysItems) {
                        sum += features.get(t).get(i);
                    }
                    double avg = sum / sysItems.size();
                    aggFeatures.add(sum);
                    aggFeatures.add(avg);
                }

                StringBuilder featureStr = new StringBuilder();
                int idx = 0;
                for (Double value : aggFeatures) {
                    value = (value - mean[idx]) / sdev[idx];
                    idx++;
                    featureStr.append(idx).append(":").append(value).append(" ");
                }
                System.out.println(String.format("%d qid:%s %s#%s", label, qid, featureStr, cmt));
            }

        }

    }

    private HashMap<String, FacetAnnotation> loadFacetAnnotation(String facetAnnotationFileName) throws IOException {

        HashMap<String, FacetAnnotation> annotations = new HashMap<>();
        BufferedReader reader = Utility.getReader(facetAnnotationFileName);
        String line;
        int fid = 0;
        while ((line = reader.readLine()) != null) {
            // a facet
            fid++;
            String[] elems = line.split("\t");
            String qid = elems[0];
            int rating = Integer.parseInt(elems[1]);
            String[] terms = elems[2].split("\\|");

            AnnotatedFacet facet = new AnnotatedFacet(rating, Integer.toString(fid), "");
            for (String t : terms) {
                facet.addTerm(t);
            }

            if (!annotations.containsKey(qid)) {
                FacetAnnotation fa = new FacetAnnotation("", qid);
                annotations.put(qid, fa);
            }
            annotations.get(qid).addFacet(facet);
        }
        reader.close();
        return annotations;
    }

    private HashMap<String, List<Double>> loadFeatures(String ftfeatureFilename) throws IOException {
        HashMap<String, List<Double>> features = new HashMap<>();
        BufferedReader reader = Utility.getReader(ftfeatureFilename);
        String line;
        int fid = 0;
        while ((line = reader.readLine()) != null) {
            String[] dataCmt = line.split("#");
            String[] elems = dataCmt[1].split("\t");
            String term = elems[2];

            String[] data = dataCmt[0].trim().split("\\s+");

            List<Double> feature = new ArrayList<>();
            for (int i = 0; i < data.length - 1; i++) {
                String[] idxValue = data[i + 1].split(":");
                feature.add(Double.parseDouble(idxValue[1]));
            }
            features.put(term, feature);
        }
        reader.close();
        return features;
    }

    private HashMap<String, Double> loadTermProb(String ftpFilename) throws IOException {
        HashMap<String, Double> probs = new HashMap<>();
        BufferedReader reader = Utility.getReader(ftpFilename);
        String line;
        int fid = 0;
        while ((line = reader.readLine()) != null) {
            String[] elems = line.split("\t");
            String term = elems[3];
            Double prob = Double.parseDouble(elems[0]);
            probs.put(term, prob);
        }
        reader.close();
        return probs;
    }

}
