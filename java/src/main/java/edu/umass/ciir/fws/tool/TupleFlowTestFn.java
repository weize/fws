/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.tool;

import java.io.PrintStream;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.Stage;

/**
 *
 * @author wkong
 */
public class TupleFlowTestFn extends AppFunction {

    private static final String name = "test-tupleflow";
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHelpString() {
         return "fws " + name + " [parameters...]\n"
                + AppFunction.getTupleFlowParameterString();
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);
        
    }

    private Job createJob(Parameters p) {
        Job job = new Job();
        
        job.add(getSplitStage(p));
        
        return job;
    }

    private Stage getSplitStage(Parameters p) {
        Stage stage = new Stage("split");
        
        return stage;
        
    }
    
}
