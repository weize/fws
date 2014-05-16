/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Collection pool files and created json file, which will be used to populate
 * the annotations database by addAolUserSessionsToMongo.rb.
 *
 * @author wkong
 */
public class CollectPoolFilesAsJsonData extends AppFunction {

    @Override
    public String getName() {
        return "collect-pool-as-json";
    }

    @Override
    public String getHelpString() {
        return "fws collect-pool-as-json config.json";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
       String poolDir = p.getString("poolDir");
       File queryFile = new File(p.getString("queryFile"));
       File outFile = new File(p.getString("poolFile"));
       TfQuery [] queries = QueryFileParser.loadQueryList(queryFile);
       
       BufferedWriter writer =Utility.getWriter(outFile);
       for(TfQuery q : queries) {
           
           Parameters pool = new Parameters();
           pool.set("userID", q.id);
           pool.set("qaspectQuery", q.text);
           
           
           // id for item
           int itemId = 0;
           
           // facets
           File poolFile = new File(Utility.getPoolFileName(poolDir, q.id));
           ArrayList<ArrayList<Parameters>> facets = new ArrayList<>();
           for(ScoredFacet f : ScoredFacet.load(poolFile)) {
               ArrayList<Parameters> facet = new ArrayList<>();
               for(ScoredItem t : f.items) {
                   itemId ++;
                   Parameters item = new Parameters();
                   item.put("query", t.item);
                   item.put("eventNo", ""+itemId);
                   facet.add(item);
               }
               facets.add(facet);
           } 
           
           pool.set("sessions", facets);
           writer.write(pool.toPrettyString());
           writer.newLine();
       }
       writer.close();
       Utility.infoWritten(outFile);
       
       
    }

}