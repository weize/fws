/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.types.TfDocumentName;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.IOException;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
@OutputClass(className = "edu.umass.ciir.fws.types.DocumentName")
public class DocumentNameFileParser extends StandardStep<FileName, TfDocumentName> {

    @Override
    public void process(FileName fileName) throws IOException {
        BufferedReader reader = Utility.getReader(fileName.filename);
        String line;
        while ((line = reader.readLine())!=null) {
            TfDocumentName docName = new TfDocumentName(line);
            processor.process(docName);
        }
    }
}
