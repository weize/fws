/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.types.CandidateList;
import edu.umass.ciir.fws.types.Term;
import java.io.IOException;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Takes a candidate and emits all list items in it as Term.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.CandidateList")
@OutputClass(className = "edu.umass.ciir.fws.types.Term")
public class CandidateListToTerms extends StandardStep<CandidateList, Term> {

    @Override
    public void process(CandidateList clist) throws IOException {
        String[] items = edu.umass.ciir.fws.clist.CandidateList.splitItemList(clist.itemList);
        for (String item : items) {
            processor.process(new Term(item));
        }
    }
}
