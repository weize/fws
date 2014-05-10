package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.tool.app.ProcessQueryApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
public class CleanCandidateLists extends ProcessQueryApp {

    @Override
    protected Class getProcessClass() {
        return CandidateListPerQueryCleaner.class;
    }

    @Override
    public String getName() {
        return "clean-candidate-lists";
    }

    /**
     * Clean and filter raw candidate lists extracted.
     *
     * @author wkong
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    public static class CandidateListPerQueryCleaner implements Processor<TfQuery> {

        CandidateListCleaner cleaner;
        String clistDir;

        public CandidateListPerQueryCleaner(TupleFlowParameters parameters) throws Exception {
            Parameters p = parameters.getJSON();
            clistDir = p.getString("clistDir");
            cleaner = new CandidateListCleaner(p);
        }

        @Override
        public void process(TfQuery query) throws IOException {
            File clistFile = new File(Utility.getCandidateListRawFileName(clistDir, query.id));

            List<CandidateList> clists = CandidateList.loadCandidateLists(clistFile);
            List<CandidateList> cleanedClists = new ArrayList<>();

            for (CandidateList clist : clists) {
                CandidateList clistClean = cleaner.clean(clist);
                if (clistClean != null) {
                    cleanedClists.add(clistClean);
                }
            }

            //output
            File clistCleanFile = new File(Utility.getCandidateListCleanFileName(clistDir, query.id));
            CandidateList.output(cleanedClists, clistCleanFile);
            Utility.infoWritten(clistCleanFile);
        }

        @Override
        public void close() throws IOException {
        }
    }

}
