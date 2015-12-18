/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.types.TfTerm;
import edu.umass.ciir.fws.utility.Utility;
import java.io.IOException;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Takes in sorted list of terms and output distinct term list
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfTerm", order = {"+term"})
@OutputClass(className = "edu.umass.ciir.fws.types.TfTerm", order = {"+term"})
public class TermUniqueReducer extends StandardStep<TfTerm, TfTerm> {

    TfTerm last = null;
    long count = 0;

    @Override
    public void process(TfTerm term) throws IOException {
        if (last == null) {
            count ++;
            processor.process(term);
        } else if (!last.term.equals(term.term)) {
            count ++;
            processor.process(term);
        }
        last = term;
    }
    
    @Override
    public void close() throws IOException {
        Utility.info("#unique terms = " + count);
        processor.close();
    }
    
}
