/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.types.Term;
import edu.umass.ciir.fws.types.TermCount;
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
@InputClass(className = "edu.umass.ciir.fws.types.Term", order = {"+term"})
@OutputClass(className = "edu.umass.ciir.fws.types.Term", order = {"+term"})
public class TermUniqueReducer extends StandardStep<Term, Term> {

    Term last = null;

    @Override
    public void process(Term term) throws IOException {
        if (last == null) {
            processor.process(term);
        } else if (!last.term.equals(term.term)) {
            processor.process(term);
        }
        last = term;
    }    
}
