/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.FileName", order = {"+qid"})
@OutputClass(className = "edu.umass.ciir.uptodate.types.Query")
public class CandidateListExtractor {
    
}
