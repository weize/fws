/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

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
public class EvalTuneGmi extends StandardStep<TfFolder, TfFolder> {

    //String predictDir;
    String trainDir;
    QueryFacetEvaluator evaluator;
    final static String model = "gmi";
    int facetTuneRank;

    public EvalTuneGmi(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        //predictDir = Utility.getFileName(gmDir, "predict");
        trainDir = Utility.getFileName(gmDir, "train");
        File facetJsonFile = new File(p.getString("facetAnnotationJson"));
        facetTuneRank = new Long(p.getLong("facetTuneRank")).intValue();
        
        evaluator = new QueryFacetEvaluator(10, facetJsonFile);
    }

    @Override
    public void process(TfFolder folder) throws IOException {
        Utility.infoProcessing(folder);
        String[] params = Utility.splitParameters(folder.id);
        String folderId = params[0];
        String predictOrTune = params[1];
        double termProbTh = Double.parseDouble(params[2]);
        double pairProbTh = Double.parseDouble(params[3]);
        String ranker = params[4];

        String folderDir = Utility.getFileName(trainDir, folderId);
        
        String evalDir = Utility.getFileName(folderDir, "eval");
        File trainQueryFile = new File(Utility.getFileName(folderDir, "train.query"));

        String tuneDir = Utility.getFileName(trainDir, folderId, "tune");
        String facetDir = tuneDir;
        
        String gmiParam = Utility.parametersToFileNameString(termProbTh, pairProbTh, ranker);
        
        File evalFile = new File(Utility.getFacetEvalFileName(evalDir, model, gmiParam, facetTuneRank));
        
        if (evalFile.exists()) {
            Utility.infoFileExists(evalFile);
            processor.process(folder);
            return;
        }
       
        evaluator.eval(trainQueryFile, facetDir, model, gmiParam, evalFile, facetTuneRank);
        Utility.infoWritten(evalFile);
        processor.process(folder);
    }
}
