/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.eval.CombinedFacetEvaluator;
import edu.umass.ciir.fws.types.TfFolderParameters;
import edu.umass.ciir.fws.utility.DirectoryUtility;
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
public class EvalFacetModelForTuning extends StandardStep<TfFolderParameters, TfFolderParameters> {

    String tuneDir;
    String runFacetDir;
    CombinedFacetEvaluator evaluator;
    String model;
    int facetTuneRank;
    boolean skipExisting;

    public EvalFacetModelForTuning(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        model = p.getString("facetModel");
        tuneDir = Utility.getFileName(p.getString("facetTuneDir"), model, "tune");
        runFacetDir = DirectoryUtility.getFacetDir(p.getString("facetRunDir"), model);
        facetTuneRank = new Long(p.getLong("facetTuneRank")).intValue();
        evaluator = new CombinedFacetEvaluator(p);
        skipExisting = p.get("skipExisting", false);
    }

    @Override
    public void process(TfFolderParameters folder) throws IOException {
        Utility.infoProcessing(folder);
        String folderId = folder.id;
        //String predictOrTune = folder.option;
        String paramsFilename = folder.parameters;

        String folderDir = Utility.getFileName(tuneDir, folderId);
        String evalDir = Utility.getFileName(folderDir, "eval");
        File trainQueryFile = new File(Utility.getFileName(folderDir, "train.query"));
        File evalFile = new File(Utility.getFacetEvalFileName(evalDir, model, paramsFilename, facetTuneRank));

        if (skipExisting && evalFile.exists()) {
            Utility.infoSkipExisting(evalFile);
        } else {
            Utility.infoOpen(evalFile);
            evaluator.eval(trainQueryFile, runFacetDir, model, paramsFilename, evalFile, facetTuneRank);
            Utility.infoWritten(evalFile);
        }
        processor.process(folder);
    }
}
