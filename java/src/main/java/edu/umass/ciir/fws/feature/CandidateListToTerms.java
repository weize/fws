/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.types.TfCandidateList;
import edu.umass.ciir.fws.types.TfTerm;
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
@InputClass(className = "edu.umass.ciir.fws.types.TfCandidateList")
@OutputClass(className = "edu.umass.ciir.fws.types.TfTerm")
public class CandidateListToTerms extends StandardStep<TfCandidateList, TfTerm> {

    @Override
    public void process(TfCandidateList clist) throws IOException {
        String[] items = edu.umass.ciir.fws.clist.CandidateList.splitItemList(clist.itemList);
        for (String item : items) {
            processor.process(new TfTerm(item));
        }
    }
}
