package edu.umass.ciir.fws.clustering.plsa;

import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * Tupleflow application that does plsa clustering
 *
 *
 * @author wkong
 */
public class PlsaClusterItems extends ProcessQueryParametersApp {

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GeneratePlsaClusterParameters.class;
    }

    @Override
    protected Class getProcessClass() {
        return PlsaClusterer.class;
    }

    @Override
    public String getName() {
        return "cluster-plsa";
    }

    /**
     *
     * @author wkong
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.Query")
    @OutputClass(className = "edu.umass.ciir.fws.types.QueryParameters")
    public static class GeneratePlsaClusterParameters extends StandardStep<TfQuery, TfQueryParameters> {

        List<Long> plsaTopicNums;

        public GeneratePlsaClusterParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            plsaTopicNums = p.getList("plsaTopicNums");
        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (long plsaTopicNum : plsaTopicNums) {
                String parameters = edu.umass.ciir.fws.utility.Utility.parametersToString(plsaTopicNum);
                processor.process(new TfQueryParameters(query.id, query.text, parameters));
            }

        }

    }
}
