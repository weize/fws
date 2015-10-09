/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws2.pilot;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class ReRankFacet extends AppFunction {
    
    @Override
    public String getName() {
        return "pilot-rerank-facets";
    }
    
    @Override
    public String getHelpString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        String test = p.getString("test");
        String predict = p.getString("predict");
        int scoreIdx = (int) p.getLong("scoreIdx");
        
        BufferedReader testReader = Utility.getReader(test);
        BufferedReader preditReader = Utility.getReader(predict);
        String line;
        HashMap<String, List<ScoredFacet>> facets = new HashMap<>();
        while ((line = testReader.readLine()) != null) {
            String line2 = preditReader.readLine();
            String[] elems = line2.split("\t");
            double score = Double.parseDouble(elems[scoreIdx]);
            
            String[] dataCmt = line.split("#");
            elems = dataCmt[1].split("\t");
            String terms = elems[1];
            //System.out.println(elems[0]);
            String qid = dataCmt[0].split("\\s+")[1].split(":")[1];
            if (!facets.containsKey(qid)) {
                facets.put(qid, new ArrayList<ScoredFacet>());
            }
            ScoredFacet facet = new ScoredFacet(score);
            for (String t : terms.split("\\|")) {
                facet.addItem(new ScoredItem(t, 0));
            }
            facets.get(qid).add(facet);
        }
        
        for (String qid : facets.keySet()) {
            List<ScoredFacet> toSort = facets.get(qid);
            Collections.sort(toSort);
            File out = new File(Utility.getFileName("rerank", qid, qid+".rerank.aspect"));
            File dir = out.getParentFile();
            if (!dir.exists()) {
                dir.mkdir();
            }
            ScoredFacet.outputAsFacets(toSort, out);
        }
    }
    
}
