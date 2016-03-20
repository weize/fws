/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.study.clist;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * select candidate list types
 *
 * @author wkong
 */
public class CandidateListsStat extends AppFunction {

    @Override
    public String getName() {
        return "stat-candidate-lists";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {

        File facetFile = new File(p.getString("facetAnnotationText"));
        HashMap<String, FacetAnnotation> fas = FacetAnnotation.loadAsMapFromTextFile(facetFile);
        String clistDir = p.getAsString("clistDir");
        String queryFile = p.getAsString("queryFile");

        List<TfQuery> queries = QueryFileParser.loadQueries(new File(queryFile));

        String all = "all";
        for (TfQuery q : queries) {
            String clistFile = Utility.getCandidateListCleanFileName(clistDir, q.id);
            List<CandidateList> clists = CandidateList.loadCandidateLists(new File(clistFile));
            HashMap<String, Integer> listNums = new HashMap<>();
            HashMap<String, Set<String>> termSets = new HashMap<>();
            for (String type : new String [] {"all", "tx", "select", "ol", "ul", "tr", "td"}) {
                listNums.put(type, 0);
                termSets.put(type, new HashSet<String>());
            }
            HashSet<String> correctTerms = new HashSet<>();
            
            for(AnnotatedFacet af : fas.get(q.id).facets) {
                correctTerms.addAll(af.terms);
            }
            
            for (CandidateList cl : clists) {
                termSets.get(cl.listType).addAll(Arrays.asList(cl.items));
                listNums.put(cl.listType, listNums.get(cl.listType) + 1);
                termSets.get(all).addAll(Arrays.asList(cl.items));
                listNums.put(all, listNums.get(all) + 1);
            }
            
            for (String type : termSets.keySet()) {
                int nLists = listNums.get(type);
                int nTerms = termSets.get(type).size();
                int nCorrect = overlap(termSets.get(type), correctTerms);
                System.out.println(String.format("%s\t%s\t%d\t%d\t%d", q.id, type, nLists, nTerms, nCorrect));
            }
        }
    }

    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private int overlap(Set<String> big, HashSet<String> small) {
        int count = 0;
        for(String t : big) {
            if (small.contains(t)) {
                count ++;
            }
        }
        
        return count;
    }
}
