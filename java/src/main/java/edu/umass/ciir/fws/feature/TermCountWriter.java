/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.clist.*;
import edu.umass.ciir.fws.types.CandidateList;
import edu.umass.ciir.fws.types.TermCount;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Tupleflow writer that write term count into one file.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TermCount")
public class TermCountWriter implements Processor<TermCount> {

    String clueDfFile;
    CandidateList last = null;
    BufferedWriter writer;
    String suffix;

    public TermCountWriter(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        clueDfFile = p.getString("clueDfFile");
        writer = Utility.getGzipWriter(clueDfFile);
    }

    @Override
    public void process(TermCount termCount) throws IOException {
        writer.write(String.format("%s\t%d\n", termCount.term, termCount.count));
    }

    @Override
    public void close() throws IOException {
        writer.close();
        System.err.println("Written in " + clueDfFile);
    }

}
