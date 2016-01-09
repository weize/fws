/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.utility;

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
public class PrfTrainTermPairModel extends StandardStep<TfFolder, TfFolder> {

    String trainDir;
    List<Long> tfIndices;
    List<Long> pfIndices;
    double alpha;
    double beta;

    public PrfTrainTermPairModel(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        String gmDir = Utility.getFileName(p.getString("facetRunDir"), "gm");
        trainDir = Utility.getFileName(gmDir, "train");
        tfIndices = p.getAsList("termFeatureIndices", Long.class);
        pfIndices = p.getAsList("pairFeatureIndices", Long.class);
        alpha = p.getDouble("gmPRFAlpha");
        alpha = p.getDouble("gmPRFBeta");
    }

    @Override
    public void process(TfFolder folder) throws IOException {
        String folderDir = Utility.getFileName(trainDir, folder.id);
        File tDataFile = new File(Utility.getFileName(folderDir, "train.t.data.gz"));
        File tModelFile = new File(Utility.getFileName(folderDir, "train.t.model"));
        File tScalerFile = new File(Utility.getFileName(folderDir, "train.t.scaler"));

        File pDataFile = new File(Utility.getFileName(folderDir, "train.p.data.gz"));
        File pModelFile = new File(Utility.getFileName(folderDir, "train.p.model"));
        File pScalerFile = new File(Utility.getFileName(folderDir, "train.p.scaler"));

        GmPRFTrainer trainer = new GmPRFTrainer(tfIndices, pfIndices);
        trainer.train(tDataFile, pDataFile, tModelFile, pModelFile, tScalerFile, pScalerFile, alpha, beta);

        Utility.infoWritten(tModelFile);
        Utility.infoWritten(tScalerFile);
        Utility.infoWritten(pModelFile);
        Utility.infoWritten(pScalerFile);

        processor.process(folder);

    }
}
