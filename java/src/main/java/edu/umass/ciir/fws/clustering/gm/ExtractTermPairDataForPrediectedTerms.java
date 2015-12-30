/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.feature.TermPairFeatureExtractor;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
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
public abstract class ExtractTermPairDataForPrediectedTerms extends StandardStep<TfQueryParameters, TfQueryParameters> {

    HashMap<String, FacetAnnotation> facetMap;
    TermPairFeatureExtractor pfExtractor;

    public ExtractTermPairDataForPrediectedTerms(TupleFlowParameters parameters) throws IOException, Exception {
        Parameters p = parameters.getJSON();
        File facetTextFile = new File(p.getString("facetAnnotationText"));
        facetMap = FacetAnnotation.loadAsMapFromTextFile(facetTextFile);
        pfExtractor = new TermPairFeatureExtractor(p);
    }

    /**
     * the dirs are different depending on whether we are doing tuning or
     * prediction
     *
     * @param foldId
     * @return
     */
    public abstract String getPredictBaseDir(String foldId);

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        Utility.infoProcessing(queryParams);
        String folderId = queryParams.parameters;
        String baseDir = getPredictBaseDir(folderId);

//        String[] params = Utility.splitParameters(queryParams.parameters);
//        String folderId = params[0];
//        String predictOrTune = params[1];
//        String tuneDir = Utility.getFileName(trainDir, folderId, "tune");
//        File predictFile = predictOrTune.equals("predict")
//                ? new File(Utility.getGmTermPredictFileName(predictDir, queryParams.id))
//                : new File(Utility.getGmTermPredictFileName(tuneDir, queryParams.id));
        File predictFile = new File(Utility.getGmTermPredictFileName(baseDir, queryParams.id));
        File dataFile = new File(Utility.getGmTermPairDataFileName(baseDir, queryParams.id));

        // output file
//        File dataFile = predictOrTune.equals("predict")
//                ? new File(Utility.getGmTermPairDataFileName(predictDir, queryParams.id))
//                : new File(Utility.getGmTermPairDataFileName(tuneDir, queryParams.id));
        pfExtractor.extract(predictFile, queryParams.id);
        Utility.infoOpen(dataFile);
        pfExtractor.output(dataFile, facetMap.get(queryParams.id));
        Utility.infoWritten(dataFile);

        processor.process(queryParams);
    }
}
