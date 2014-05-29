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
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class TermPredictor extends StandardStep<TfQueryParameters, TfQueryParameters> {

    String predictDir;
    String trainDir;
    List<Long> tfIndices;

    public TermPredictor(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        predictDir = Utility.getFileName(gmDir, "predict");
        trainDir = Utility.getFileName(gmDir, "train");
        tfIndices = p.getAsList("termFeatureIndices", Long.class);
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        String [] params = Utility.splitParameters(queryParams.parameters);
        String folderId = params[0];
        String predictOrTune = params[1];
        String tuneDir = Utility.getFileName(trainDir, folderId, "tune");
        File dataFile = new File(Utility.getGmTermDataFileName(predictDir, queryParams.id));
        File predictFile = predictOrTune.equals("predict") ?
                new File(Utility.getGmTermPredictFileName(predictDir, queryParams.id)) :
                new File(Utility.getGmTermPredictFileName(tuneDir, queryParams.id));
        
        Utility.createDirectoryForFile(predictFile);

        String folderDir = Utility.getFileName(trainDir, folderId);
        File modelFile = new File(Utility.getFileName(folderDir, "train.t.model"));
        File scalerFile = new File(Utility.getFileName(folderDir, "train.t.scaler"));

        Utility.infoProcessing(dataFile);
        LinearRegressionModel model = new LinearRegressionModel(tfIndices);
        model.predict(dataFile, modelFile, scalerFile, predictFile);
        Utility.infoWritten(predictFile);

        processor.process(queryParams);
    }

}
