/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.study;

import edu.umass.ciir.fws.anntation.FeedbackTerm;
import edu.umass.ciir.fws.clustering.FacetModelParamGenerator;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.eval.FfeedbackTimeEstimator;
import edu.umass.ciir.fws.ffeedback.FacetFeedback;
import edu.umass.ciir.fws.ffeedback.QueryMetricsTime;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class CmpFacetsAccNumTermOverFacets extends AppFunction {
    
    @Override
    public String getName() {
        return "cmp-facet-term-num";
    }
    
    @Override
    public String getHelpString() {
        return "fws cmp-facet-term-num\n";
    }
    
    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        List<String> facetSrcs = p.getAsList("facetSources");
        String facetDir = p.getString("facetDir");
        FacetModelParamGenerator facetParamGen = new FacetModelParamGenerator(p);
        List<TfQuery> queries = QueryFileParser.loadQueries(new File(p.getString("queryFile")));
        
        for (String facetSrc : facetSrcs) {
            List<String> facetParams = facetParamGen.getParams(facetSrc);
            for (String facetParam : facetParams) {
                File outfile = new File(geOutFileName("../exp/data/cmp-facet", facetSrc, facetParam));
                process(facetSrc, facetParam, queries, facetDir, outfile);
            }
        }
        
    }
    
    public static String geOutFileName(String dir, String facetSource, String facetParams) {
        facetParams = Utility.parametersToFileNameString(facetParams);
        String facetName = facetParams.isEmpty() ? String.format("%s", facetSource)
                : String.format("%s.%s", facetSource, facetParams);
        String name = String.format("%s.accuTermOverFacet", facetName);
        return Utility.getFileName(dir, name);
    }
    
    private void process(String facetSrc, String facetParam, List<TfQuery> queries, String facetDir, File outfile) throws IOException {
        TreeMap<String, List<QueryMetricsTime>> qmts = new TreeMap<>();
        for (TfQuery q : queries) {
            File facetFile = new File(Utility.getFacetFileName(Utility.getFileName(facetDir, facetSrc, "facet"), q.id, facetSrc, facetParam));
            List<ScoredFacet> facets = ScoredFacet.loadFacets(facetFile);
            int termCount = 0;
            int facetCount = 0;
            ArrayList<QueryMetricsTime> qmtList = new ArrayList<>();
            QueryMetricsTime first = new QueryMetricsTime(q.id, new double[]{termCount}, facetCount);
            qmtList.add(first);
            for (ScoredFacet f : facets) {
                facetCount++;
                termCount += f.size();
                QueryMetricsTime qmt = new QueryMetricsTime(q.id, new double[]{termCount}, facetCount);
                qmtList.add(qmt);
            }
            qmts.put(q.id, qmtList);
        }
        
        List<QueryMetricsTime> avgByQuery = QueryMetricsTime.avgQmts(qmts, "all");
        QueryMetricsTime.outputAvg(outfile, avgByQuery);
        Utility.infoWritten(outfile);
    }
    
}
