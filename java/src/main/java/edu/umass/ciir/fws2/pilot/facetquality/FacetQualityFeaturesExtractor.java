/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws2.pilot.facetquality;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.eval.PrfEvaluator;
import edu.umass.ciir.fws.eval.CombinedFacetEvaluator;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
public class FacetQualityFeaturesExtractor implements Processor<TfQuery> {

    String facetDir;
    String facetModel;
    String facetParam;
    String gmPredictDir;
    String fqDir; // facet qaulity dir
    HashMap<String, FacetAnnotation> facetAnnotationMap;
    FacetAnnotation facetAnnotation;

    List<ScoredFacet> facets;
    HashMap<String, Double> termProb;
    TermAndPairProbMap tpProbMap;

    public FacetQualityFeaturesExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        facetModel = p.getString("facetModel");
        facetDir = Utility.getFileName(p.getString("facetDir"), facetModel, "facet");
        facetParam = p.getAsString("facetParam");
        fqDir = p.getString("fqDir");
        File annotatedFacetTextFile = new File(p.getString("facetAnnotationText"));
        gmPredictDir = Utility.getFileName(p.getString("facetRunDir"), "gm", "predict");

        facetAnnotationMap = FacetAnnotation.loadAsMapFromTextFile(annotatedFacetTextFile);

    }

    @Override
    public void process(TfQuery query) throws IOException {

        //output
        File outfile = new File(Utility.getFileNameWithSuffix(fqDir, "feature",
                query.id, "f.quality.feature"));
        BufferedWriter writer = Utility.getWriter(outfile);

        // load facets
        File facetFile = new File(Utility.getFacetFileName(facetDir, query.id, facetModel, facetParam));
        facets = ScoredFacet.loadFacets(facetFile);

        // load facet annotation
        facetAnnotation = facetAnnotationMap.get(query.id);

        // load item and pair prob
        tpProbMap = new TermAndPairProbMap();
        tpProbMap.termIdMap.clear();
        for (ScoredFacet facet : facets) {
            for (ScoredItem item : facet.items) {
                tpProbMap.addTerm(item.item);
            }
        }
        File tPredictFile = new File(Utility.getFileName(gmPredictDir, query.id, String.format("%s.t.predict", query.id)));
        File pPredictFile = new File(Utility.getFileName(gmPredictDir, query.id, String.format("%s.p.predict.gz", query.id)));
        tpProbMap.loadTermProb(tPredictFile, true);
        tpProbMap.loadTermPairProb(pPredictFile);

        for (ScoredFacet facet : facets) {
            FacetFeatures features = new FacetFeatures();
            extractQaulity(facet, features);
            extractFeatures(facet, facets, features);
            writer.write(String.format("%s\t%s\n", features.featuresToString(), facet.getItemList()));
        }

        writer.close();
        Utility.infoWritten(outfile);

    }

    @Override
    public void close() throws IOException {

    }

    private void extractQaulity(ScoredFacet facet, FacetFeatures features) {
        // extract best precision, recall, and F1
        HashSet<String> sysItems = new HashSet<>();
        for (ScoredItem item : facet.items) {
            sysItems.add(item.item);
        }

        double optimalF1 = 0;
        double optimalWf1 = 0;
        double optimalP = 0;
        double optimalR = 0;
        
        double weightSum = 0;
        for (AnnotatedFacet af : facetAnnotation.facets) {
            weightSum += af.size() * af.rating;
        }
        for (AnnotatedFacet af : facetAnnotation.facets) {
            int count = 0;
            for (String t : af.terms) {
                if (sysItems.contains(t)) {
                    count++;
                }
            }
            double precision = (double) count / sysItems.size();
            double recall = (double) count / af.size();
            double f1 = CombinedFacetEvaluator.f1(precision, recall);
            double wf1 = f1 * af.terms.size() * af.rating / weightSum;
            optimalF1 = Math.max(f1, optimalF1);
            optimalWf1 = Math.max(wf1, optimalWf1);
            optimalP = Math.max(precision, optimalP);
            optimalR = Math.max(recall, optimalR);
        }

        features.setFeature(optimalP, FacetFeatures._qPrecision);
        features.setFeature(optimalR, FacetFeatures._qRecall);
        features.setFeature(optimalF1, FacetFeatures._qF1);
        features.setFeature(optimalWf1, FacetFeatures._qWf1);
        
//        // single facet wPRF
//        PrfEvaluator prfEvaluator = new PrfEvaluator(1);
//        List<ScoredFacet> system = new ArrayList<>();
//        system.add(facet);
//        double [] scores = prfEvaluator.eval(facetAnnotation.facets, system, 1);
        
        

    }

    private void extractFeatures(ScoredFacet facet, List<ScoredFacet> facets, FacetFeatures features) {
        // term features
        double tProbSum = 0;
        double tProbAvg;
        double tProbMax = Double.NEGATIVE_INFINITY;
        double tProbMin = Double.POSITIVE_INFINITY;
        int tSize = facet.items.size();
        for (ScoredItem item : facet.items) {
            double tProb = tpProbMap.getTermProb(item.item);
            tProbSum += tProb;
            tProbMax = Math.max(tProb, tProbMax);
            tProbMin = Math.min(tProb, tProbMin);
        }
        tProbAvg = tProbSum / tSize;
        features.setFeature(tSize, FacetFeatures._tSize);
        features.setFeature(tProbSum, FacetFeatures._tProbSum);
        features.setFeature(tProbAvg, FacetFeatures._tProbAvg);
        features.setFeature(tProbMin, FacetFeatures._tProbMin);
        features.setFeature(tProbMax, FacetFeatures._tProbMax);

        // term pair features
        int pIntraSize = 0;
        double pIntraProbSum = 0;
        double pIntraProbAvg;
        double pIntraProbMin = Double.POSITIVE_INFINITY;
        double pIntraProbMax = Double.NEGATIVE_INFINITY;

        int pInterSize = 0;
        double pInterProbSum = 0;
        double pInterProbAvg;
        double pInterProbMin = Double.POSITIVE_INFINITY;
        double pInterProbMax = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < tSize; i++) {
            String term1 = facet.items.get(i).item;
            for (int j = i + 1; j < tSize; j++) {
                String term2 = facet.items.get(j).item;
                double pProb = tpProbMap.getPairProb(term1, term2);
                pIntraSize++;
                pIntraProbSum += pProb;
                pIntraProbMin = Math.min(pProb, pIntraProbMin);
                pIntraProbMax = Math.max(pProb, pIntraProbMax);
            }

            for (ScoredFacet f : facets) {
                if (f != facet) {
                    for (ScoredItem item : f.items) {
                        double pProb = tpProbMap.getPairProb(term1, item.item);
                        pInterSize++;
                        pInterProbSum += pProb;
                        pInterProbMin = Math.min(pProb, pInterProbMin);
                        pInterProbMax = Math.max(pProb, pInterProbMax);
                    }
                }
            }
        }

        pInterProbAvg = pInterProbSum / (double) pInterSize;
        pIntraProbAvg = pIntraProbSum / (double) pIntraSize;
        
        features.setFeature(pInterSize, FacetFeatures._pInterSize);
        features.setFeature(pIntraSize, FacetFeatures._pIntraSize);
        features.setFeature(pInterProbSum, FacetFeatures._pInterProbSum);
        features.setFeature(pIntraProbSum, FacetFeatures._pIntraProbSum);
        features.setFeature(pInterProbAvg, FacetFeatures._pInterProbAvg);
        features.setFeature(pIntraProbAvg, FacetFeatures._pIntraProbAvg);
        features.setFeature(pInterProbMin, FacetFeatures._pInterProbMin);
        features.setFeature(pIntraProbMin, FacetFeatures._pIntraProbMin);
        features.setFeature(pInterProbMax, FacetFeatures._pInterProbMax);
        features.setFeature(pIntraProbMax, FacetFeatures._pIntraProbMax);

    }
}
