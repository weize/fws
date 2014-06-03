/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

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
    public File expansionIdFile;

    public ExpansionDirectory(Parameters p) {
        allExpansionDir = p.getString("expansionDir");
        runDir = Utility.getFileName(allExpansionDir, "run");
        expansionIdFile = new File(Utility.getFileName(allExpansionDir, "expansion.id.gz"));
    }

    public File getExpansionFile(String source, String expansionModel) {
        return new File(Utility.getFileNameWithSuffix(allExpansionDir, source, "expansion." + expansionModel, "gz"));
    }

}
