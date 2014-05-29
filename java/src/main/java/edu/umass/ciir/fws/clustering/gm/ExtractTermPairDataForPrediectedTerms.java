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
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class ExtractTermPairDataForPrediectedTerms extends StandardStep<TfQueryParameters, TfQueryParameters> {

    String predictDir;
    String trainDir;
    HashMap<String, FacetAnnotation> facetMap;
    TermPairFeatureExtractor pfExtractor;

    public ExtractTermPairDataForPrediectedTerms(TupleFlowParameters parameters) throws IOException, Exception {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        predictDir = Utility.getFileName(gmDir, "predict");
        trainDir = Utility.getFileName(gmDir, "train");
        File facetJsonFile = new File(p.getString("facetAnnotationJson"));
        facetMap = FacetAnnotation.loadAsMap(facetJsonFile);
        pfExtractor = new TermPairFeatureExtractor(p);
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        String [] params = Utility.splitParameters(queryParams.parameters);
        String folderId = params[0];
        String predictOrTune = params[1];
        String tuneDir = Utility.getFileName(trainDir, folderId, "tune");
        File predictFile = predictOrTune.equals("predict") ?
                new File(Utility.getGmTermPredictFileName(predictDir, queryParams.id)) :
                new File(Utility.getGmTermPredictFileName(tuneDir, queryParams.id));
        
        Utility.infoProcessing(queryParams);
        // output file
        File dataFile = predictOrTune.equals("predict") ?
                new File(Utility.getGmTermPairDataFileName(predictDir, queryParams.id)) :
                new File(Utility.getGmTermPairDataFileName(tuneDir, queryParams.id));
        
        pfExtractor.extract(predictFile, queryParams.id);
        pfExtractor.output(dataFile, facetMap.get(queryParams.id));
        Utility.infoWritten(dataFile);

        processor.process(queryParams);
    }
}
