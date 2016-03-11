/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.study;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.ffeedback.FacetFeedback;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * select candidate list types
 *
 * @author wkong
 */
public class SelectCandidateLists extends AppFunction {

    @Override
    public String getName() {
        return "select-candidate-lists";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        List<String> types = p.getAsList("patterns");
        String clistBaseDir = p.getAsString("clistBaseDir");
        String clistDir = p.getAsString("clistDir");

        String queryFile = p.getAsString("queryFile");

        List<TfQuery> queries = QueryFileParser.loadQueries(new File(queryFile));

        for (TfQuery q : queries) {
            String clistBaseFile = Utility.getCandidateListCleanFileName(clistBaseDir, q.id);
            String clistFilteredFile = Utility.getCandidateListCleanFileName(clistDir, q.id);
            List<CandidateList> clists = CandidateList.loadCandidateLists(new File(clistBaseFile));
            ArrayList<CandidateList> selected = new ArrayList<>();
            for (CandidateList cl : clists) {
                if (isSelected(cl, types)) {
                    selected.add(cl);
                }
            }
            CandidateList.output(selected, new File(clistFilteredFile));
        }
    }

    private boolean isSelected(CandidateList cl, List<String> types) {
        for (String type : types) {
            if (cl.listType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
