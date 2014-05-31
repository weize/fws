/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.eval.QueryFacetEvaluator;
import edu.umass.ciir.fws.types.TfFolder;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
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
public class EvalFacetModelForTuning extends StandardStep<TfFolder, TfFolder> {

    String tuneDir;
    String runFacetDir;
    QueryFacetEvaluator evaluator;
    String model;

    public EvalFacetModelForTuning(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        model = p.getString("facetModel");
        String modelDir = Utility.getFileName(p.getString("facetDir"), model);
        tuneDir = Utility.getFileName(modelDir, "tune");
        runFacetDir = Utility.getFileName(modelDir, "run", "facet");
        File facetJsonFile = new File(p.getString("facetAnnotationJson"));

        evaluator = new QueryFacetEvaluator(10, facetJsonFile);
    }

    @Override
    public void process(TfFolder folder) throws IOException {
        Utility.infoProcessing(folder);
        String[] params = Utility.splitParameters(folder.id);
        String folderId = params[0];
        String predictOrTune = params[1];

        String folderDir = Utility.getFileName(tuneDir, folderId);
        String evalDir = Utility.getFileName(folderDir, "eval");
        File trainQueryFile = new File(Utility.getFileName(folderDir, "train.query"));
        
        String param = "";

        if (model.equals("plsa")) {
            long topicNum = Long.parseLong(params[2]);
            long termNum = Long.parseLong(params[3]);
            param = Utility.parametersToFileNameString(topicNum, termNum);
        } else if (model.equals("lda")) {
            long topicNum = Long.parseLong(params[2]);
            long termNum = Long.parseLong(params[3]);
            param = Utility.parametersToFileNameString(topicNum, termNum);

        } else if (model.equals("qd")) {
            double qdDistanceMax = Double.parseDouble(params[2]);
            double qdWebsiteCountMin = Double.parseDouble(params[3]);
            double qdItemRatio = Double.parseDouble(params[4]);
            param = Utility.parametersToFileNameString(qdDistanceMax, qdWebsiteCountMin, qdItemRatio);
        }

        File evalFile = new File(Utility.getFacetEvalFileName(evalDir, model, param));
        
        if (evalFile.exists()) {
            Utility.infoFileExists(evalFile);
            processor.process(folder);
            return;
        }
        
        evaluator.eval(trainQueryFile, runFacetDir, model, param, evalFile);
        Utility.infoWritten(evalFile);
        processor.process(folder);
    }
}
