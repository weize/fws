/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.types.TfQueryExpansion;
import java.io.IOException;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic", order = { "+qid", "+model", "+expId" })
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansionSubtopic", order = { "+qid", "+model", "+expId" })
public class UniqueQueryExpansion extends StandardStep<TfQueryExpansion, TfQueryExpansion>{
    TfQueryExpansion last = null;

    @Override
    public void process(TfQueryExpansion qe) throws IOException {
        if (last == null) {
            last = qe;
        } else if (!qe.qid.equals(last.qid) 
                || !qe.model.equals(last.model)
                || qe.expId != last.expId) {
            processor.process(last);
            last = qe;
        }
    }
    
    public void close()  throws IOException { 
        if (last != null) {
            processor.process(last);
        }
        processor.close();
    }

}
