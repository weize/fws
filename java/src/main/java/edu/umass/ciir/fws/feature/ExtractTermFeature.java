package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.tool.app.ProcessQueryApp;
import edu.umass.ciir.fws.utility.Utility;
import java.io.PrintStream;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.execution.Job;

/**
 * Tupleflow application that extract facet term features.
 *
 *
 * @author wkong
 */
public class ExtractTermFeature extends ProcessQueryApp {
    
    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        String featureDir = p.getString("featureDir");
        Utility.createDirectory(featureDir);
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);

    }

    @Override
    protected Class getProcessClass() {
        return TermFeaturesExtractor.class;
    }

    @Override
    public String getName() {
        return "extract-term-feature";
    }
}
