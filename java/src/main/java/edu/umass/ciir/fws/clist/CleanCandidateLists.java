package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.tool.app.ProcessQueryApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        return CandidateListCleaner.class;
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
    public static class CandidateListCleaner implements Processor<TfQuery> {

        Set<String> stopwords = new HashSet<>();
        String clistDir;

        public CandidateListCleaner(TupleFlowParameters parameters) throws Exception {
            Parameters p = parameters.getJSON();
            clistDir = p.getString("clistDir");
            // load stopwords
            String stopwordsFile = p.getString("stopwordsFile");
            stopwords = Utility.readFileToStringSet(new File(stopwordsFile));

        }

        @Override
        public void process(TfQuery query) throws IOException {
            File clistFile = new File(Utility.getCandidateListRawFileName(clistDir, query.id));

            List<CandidateList> clists = CandidateList.loadCandidateLists(clistFile);
            List<CandidateList> cleanedClists = new ArrayList<>();

            for (CandidateList clist : clists) {
                ArrayList<String> itemsCleaned = new ArrayList<>();
                HashSet<String> itemCleanedSet = new HashSet(); // constinct
                for (String item : clist.items) {
                    String itemCleaned = TextProcessing.clean(item);
                    if (itemCleanedSet.contains(itemCleaned)) {
                        continue;
                    }
                    if (isValidItem(itemCleaned)) {
                        itemsCleaned.add(itemCleaned);
                        itemCleanedSet.add(itemCleaned);
                    }
                }

                if (isValidItemList(itemsCleaned)) {
                    cleanedClists.add(new CandidateList(
                            clist.qid, clist.docRank, clist.docName, clist.listType, itemsCleaned));
                }
            }

            //output
            File clistCleanFile = new File(Utility.getCandidateListCleanFileName(clistDir, query.id));
            CandidateList.output(cleanedClists, clistCleanFile);
            Utility.infoWritten(clistCleanFile);

        }

        private boolean isValidItem(String item) {
            if (item.length() < 1) {
                return false;
            }

            if (stopwords.contains(item)) {
                return false;
            }

            // number of words
            int length = item.split("\\s+").length;
            return length <= edu.umass.ciir.fws.clist.CandidateList.MAX_TERM_SIZE;
        }

        private boolean isValidItemList(List<String> items) {
            int size = items.size();

            return size > 1 && size <= 200;
        }

        @Override
        public void close() throws IOException {
        }
    }

}
