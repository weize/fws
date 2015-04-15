/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.experiment;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.anntation.FacetAnnotation;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class FacetAnnotationStat extends AppFunction {

    @Override
    public String getName() {
        return "stat-facets";
    }

    @Override
    public String getHelpString() {
        return "fws stat-facets";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        facetAnnotationStat(p, output);
        facetPoolStat(p, output);

    }

    private void facetAnnotationStat(Parameters p, PrintStream output) throws IOException {
        File facetFile = new File(p.getString("facetAnnotationJson"));
        List<FacetAnnotation> fas = FacetAnnotation.load(facetFile);

        File outfile = new File("../exp/data/stat/facet.stats");
        BufferedWriter writer = Utility.getWriter(outfile);
        for (FacetAnnotation fa : fas) {
            // for a query
            int goodTerm = 0, goodFacet = 0;
            int fairTerm = 0, fairFacet = 0;
            for (AnnotatedFacet f : fa.facets) {
                if (f.rating == 1) {
                    fairFacet++;
                    fairTerm += f.size();
                } else if (f.rating == 2) {
                    goodFacet++;
                    goodTerm += f.size();
                }
            }
            writer.write(String.format("%s\t%d\t%d\t%d\t%d\n", fa.qid, fairTerm, fairFacet, goodTerm, goodFacet));
        }
        writer.close();
        Utility.infoWritten(outfile);
    }

    private void facetPoolStat(Parameters p, PrintStream output) throws IOException {
        File queryFile = new File(p.getString("queryFile"));
        List<TfQuery> queries = QueryFileParser.loadQueries(queryFile);
        
        File outfile = new File("../exp/data/stat/facet-pool.stats");
        BufferedWriter writer = Utility.getWriter(outfile);
        
        for (TfQuery q : queries) {
            File poolFile = new File(Utility.getPoolFileName(p.getString("poolDir"), q.id));
            List<ScoredFacet> facets = ScoredFacet.loadFacets(poolFile);
            int tCount = 0;
            for(ScoredFacet f : facets){
                tCount += f.size();
            }
            writer.write(q.id+"\t"+tCount+"\t"+facets.size());
            writer.newLine();
        }
        
        writer.close();
        Utility.infoWritten(outfile);
    }

}
