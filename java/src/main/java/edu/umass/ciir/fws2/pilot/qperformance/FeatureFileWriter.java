/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umass.ciir.fws2.pilot.qperformance;

import edu.umass.ciir.fws.types.TfQueryParameters;
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
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters", order = {"+id"})
public class FeatureFileWriter implements Processor<TfQueryParameters> {

    BufferedWriter writer;
    File outfile;
    public FeatureFileWriter(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        String filename = p.getString("qpFeature");
        outfile = new File(filename);
        writer = Utility.getWriter(filename);
        
    }
    @Override
    public void process(TfQueryParameters qp) throws IOException {
        writer.write(String.format("%s\t%s\n", qp.id, qp.parameters));
    }

    @Override
    public void close() throws IOException {
        writer.close();
        Utility.infoWritten(outfile);
    }
    
}
