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
 * parameters: id
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public abstract class TermPredictor extends StandardStep<TfQueryParameters, TfQueryParameters> {

    String dataDir;
    String trainDir;
    List<Long> tfIndices;

    public TermPredictor(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        dataDir = Utility.getFileName(gmDir, "predict"); // for get the data file
        trainDir = Utility.getFileName(gmDir, "train");
        tfIndices = p.getAsList("termFeatureIndices", Long.class);
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
        File dataFile = new File(Utility.getGmTermDataFileName(dataDir, queryParams.id));
        String baseDir = getPredictBaseDir(folderId);
        File predictFile = new File(Utility.getGmTermPredictFileName(baseDir, queryParams.id));

// String tuneDir = Utility.getFileName(trainDir, folderId, "tune");
//                predictOrTune.equals("predict") ?
//                new File(Utility.getGmTermPredictFileName(predictDir, queryParams.id)) :
//                new File(Utility.getGmTermPredictFileName(tuneDir, queryParams.id));
        String folderDir = Utility.getFileName(trainDir, folderId);
        File modelFile = new File(Utility.getFileName(folderDir, "train.t.model"));
        File scalerFile = new File(Utility.getFileName(folderDir, "train.t.scaler"));

        Utility.createDirectoryForFile(predictFile);
        Utility.infoOpen(predictFile);
        LinearRegressionModel model = new LinearRegressionModel(tfIndices);
        model.predict(dataFile, modelFile, scalerFile, predictFile);
        Utility.infoWritten(predictFile);

        processor.process(queryParams);
    }

}
