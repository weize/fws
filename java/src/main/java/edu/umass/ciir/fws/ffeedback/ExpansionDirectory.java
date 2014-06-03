/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.types.TfFacetFeedbackParams;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class ExpansionDirectory {

    public String allExpansionDir;
    public String runDir;
    public String evalDir;
    public File expansionIdFile;

    public ExpansionDirectory(Parameters p) {
        allExpansionDir = p.getString("expansionDir");
        runDir = Utility.getFileName(allExpansionDir, "run");
        evalDir = Utility.getFileName(allExpansionDir, "eval");
        expansionIdFile = new File(Utility.getFileName(allExpansionDir, "expansion.id.gz"));
    }

    public File getExpansionFile(String source, String expansionModel) {
        return new File(Utility.getFileNameWithSuffix(allExpansionDir, source, "expansion." + expansionModel, "gz"));
    }

    public File getExpansionEvalFile(String source, String expansionModel) {
        return new File(Utility.getFileNameWithSuffix(allExpansionDir, source, "expansion." + expansionModel, "eval"));
    }

    public File getExpansionIdFile(String qid) {
        return new File(Utility.getFileName(allExpansionDir, "id", "expansion." + qid + ".id"));
    }

    public File getExpansionEvalImprvFile(String source, String expansionModel) {
        return new File(Utility.getFileNameWithSuffix(allExpansionDir, source, "expansion." + expansionModel, "eval.imprv"));
    }

    File getExpansionFile(TfFacetFeedbackParams param, String expansionModel) {
        File feedbackFile = new File(Utility.getFeedbackFileName("test", param));
        String dirName = feedbackFile.getName();
        return new File(Utility.getFileNameWithSuffix(allExpansionDir, param.facetSource, dirName, "expansion." + expansionModel, "gz"));

    }

}
