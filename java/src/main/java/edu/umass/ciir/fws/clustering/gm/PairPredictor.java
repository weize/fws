/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.gm.lr.LinearRegressionModel;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.List;
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
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public abstract class PairPredictor extends StandardStep<TfQueryParameters, TfQueryParameters> {

    String trainDir;
    List<Long> indices; // indices for selected features
    List<Double> termProbThs;
    List<Double> pairProbThs;

    public PairPredictor(TupleFlowParameters parameters) {

        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        trainDir = Utility.getFileName(gmDir, "train");
        indices = p.getAsList("pairFeatureIndices", Long.class);
    }

    /**
     *
     * @param foldId first argument is qid, the second should be foldId if it is
     * for tuning
     * @return
     */
    public abstract String getPredictBaseDir(String foldId);

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        String folderId = queryParams.parameters;
        String baseDir = getPredictBaseDir(folderId);
        File dataFile = new File(Utility.getGmTermPairDataFileName(baseDir, queryParams.id));
        File predictFile = new File(Utility.getGmTermPairPredictFileName(baseDir, queryParams.id));

//        String[] params = Utility.splitParameters(queryParams.parameters);
//        String folderId = params[0];
//        String predictOrTune = params[1];
//        String tuneDir = Utility.getFileName(trainDir, folderId, "tune");
//        File dataFile = predictOrTune.equals("predict")
//                ? new File(Utility.getGmTermPairDataFileName(predictDir, queryParams.id))
//                : new File(Utility.getGmTermPairDataFileName(tuneDir, queryParams.id));
//        File predictFile = predictOrTune.equals("predict")
//                ? new File(Utility.getGmTermPairPredictFileName(predictDir, queryParams.id))
//                : new File(Utility.getGmTermPairPredictFileName(tuneDir, queryParams.id));
        String folderDir = Utility.getFileName(trainDir, folderId);
        File modelFile = new File(Utility.getFileName(folderDir, "train.p.model"));
        File scalerFile = new File(Utility.getFileName(folderDir, "train.p.scaler"));

        Utility.infoProcessing(dataFile);
        LinearRegressionModel model = new LinearRegressionModel(indices);
        model.predict(dataFile, modelFile, scalerFile, predictFile);
        Utility.infoWritten(predictFile);

        processor.process(queryParams);
    }

}
