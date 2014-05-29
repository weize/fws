/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.gm.lr.LinearRegressionModel;
import edu.umass.ciir.fws.types.TfFolder;
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
@InputClass(className = "edu.umass.ciir.fws.types.TfFolder")
@OutputClass(className = "edu.umass.ciir.fws.types.TfFolder")
public class TrainPairModel extends StandardStep<TfFolder, TfFolder> {

    String trainDir;
    List<Long> pfIndices;

    public TrainPairModel(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        trainDir = Utility.getFileName(gmDir, "train");
        pfIndices = p.getAsList("pairFeatureIndices", Long.class);
    }

    @Override
    public void process(TfFolder folder) throws IOException {
        String folderDir = Utility.getFileName(trainDir, folder.id);
        File dataFile = new File(Utility.getFileName(folderDir, "train.p.data.gz"));
        File modelFile = new File(Utility.getFileName(folderDir, "train.p.model"));
        File scalerFile = new File(Utility.getFileName(folderDir, "train.p.scaler"));

        LinearRegressionModel tModel = new LinearRegressionModel(pfIndices);
        tModel.train(dataFile, modelFile, scalerFile);
        Utility.infoWritten(modelFile);
        Utility.infoWritten(scalerFile);

        processor.process(folder);

    }
}
