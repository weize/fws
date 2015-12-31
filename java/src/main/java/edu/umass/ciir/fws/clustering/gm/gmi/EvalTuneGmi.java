/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmi;

import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiFacetParameters;
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
public class EvalTuneGmi extends StandardStep<TfFolderParameters, TfFolderParameters> {

    public final static String model = "gmi";

    //String predictDir;
    String gmTrainDir;
    CombinedFacetEvaluator evaluator;
    int facetTuneRank;
    String gmiRunDir;
    String gmiTuneDir;
    boolean skipExisting;

    public EvalTuneGmi(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        //predictDir = Utility.getFileName(gmDir, "predict");
        gmTrainDir = Utility.getFileName(gmDir, "train");
        gmiRunDir = DirectoryUtility.getModelRunDir(p.getString("facetRunDir"), model);
        gmiTuneDir = Utility.getFileName(p.getString("facetTuneDir"), model, "tune");
        skipExisting = p.get("skipExisting", false);

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

        File trainQueryFile = new File(Utility.getFileName(gmTrainDir, folderId, "train.query"));

        String evalDir = Utility.getFileName(gmiTuneDir, folderId, "eval");
        String facetDir = Utility.getFileName(gmiRunDir, folderId, "facet");

        String paramFilename = params.toFilenameString();

        File evalFile = new File(Utility.getFacetEvalFileName(evalDir, model, paramFilename, facetTuneRank));

        if (skipExisting && evalFile.exists()) {
            Utility.infoSkipExisting(evalFile);
        } else {
            Utility.infoOpen(evalFile);
            Utility.createDirectoryForFile(evalFile);
            evaluator.eval(trainQueryFile, facetDir, model, paramFilename, evalFile, facetTuneRank);
            Utility.infoWritten(evalFile);
        }

        processor.process(folder);
    }
}
