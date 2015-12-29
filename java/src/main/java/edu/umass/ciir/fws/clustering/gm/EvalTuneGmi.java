/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiFacetParameters;
import edu.umass.ciir.fws.eval.CombinedFacetEvaluator;
import edu.umass.ciir.fws.types.TfFolderParameters;
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
@InputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
@OutputClass(className = "edu.umass.ciir.fws.types.TfFolderParameters")
public class EvalTuneGmi extends StandardStep<TfFolderParameters, TfFolderParameters> {

    //String predictDir;
    String trainDir;
    CombinedFacetEvaluator evaluator;
    final static String model = "gmi";
    int facetTuneRank;

    public EvalTuneGmi(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        //predictDir = Utility.getFileName(gmDir, "predict");
        trainDir = Utility.getFileName(gmDir, "train");
        //File facetJsonFile = new File(p.getString("facetAnnotationJson"));
        facetTuneRank = new Long(p.getLong("facetTuneRank")).intValue();
        evaluator = new CombinedFacetEvaluator(p);
    }

    @Override
    public void process(TfFolderParameters folder) throws IOException {
        Utility.infoProcessing(folder);
        String folderId = folder.id;        
        String predictOrTune = folder.option;
        GmiFacetParameters params = new GmiFacetParameters(folder.parameters);

        String folderDir = Utility.getFileName(trainDir, folderId);
        
        String evalDir = Utility.getFileName(folderDir, "eval");
        File trainQueryFile = new File(Utility.getFileName(folderDir, "train.query"));

        String tuneDir = Utility.getFileName(trainDir, folderId, "tune");
        String facetDir = tuneDir;
        
        String paramFilename = params.toFilenameString();
        
        File evalFile = new File(Utility.getFacetEvalFileName(evalDir, model, paramFilename, facetTuneRank));
        
//        if (evalFile.exists()) {
//            Utility.infoFileExists(evalFile);
//            processor.process(folder);
//            return;
//        }
       
        Utility.infoOpen(evalFile);
        evaluator.eval(trainQueryFile, facetDir, model, paramFilename, evalFile, facetTuneRank);
        Utility.infoWritten(evalFile);
        processor.process(folder);
    }
}
