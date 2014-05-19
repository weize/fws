/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.tool.app.ProcessQueryApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * TupleFlow job that pool top facets from different runs and create annotation
 * set.
 *
 * @author wkong
 */
public class PoolFacets extends ProcessQueryApp {

    @Override
    protected Class getProcessClass() {
        return FacetPooler.class;
    }

    @Override
    public String getName() {
        return "pool-facets";
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    public static class FacetPooler implements Processor<TfQuery> {

        String poolDir;
        List<List<Object>> runs; // [[dir, model, param1, param2, ...], ]
        long facetTopNum;

        public FacetPooler(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            poolDir = p.getString("poolDir");
            runs = p.getList("poolRuns");
            facetTopNum = p.getLong("poolFacetTopNum");
        }

        @Override
        public void process(TfQuery query) throws IOException {
            System.err.println("processing " + query.id);
            List<List<ScoredFacet>> facetsList = new ArrayList<>();
            for (List<Object> run : runs) {
                File facetFile = new File(Utility.getFacetFileName(run, query.id));
                Utility.infoProcessing(facetFile);
                facetsList.add(ScoredFacet.load(facetFile));
            }

            List<ScoredFacet> facetPool = poolFacets(facetsList);
            File poolFile = new File(Utility.getPoolFileName(poolDir, query.id));
            ScoredFacet.outputAsFacets(facetPool, poolFile);
            Utility.infoWritten(poolFile);
        }

        @Override
        public void close() throws IOException {
        }

        /**
         * *
         * Each time randomly select a facet from the top of each run. Terms
         * seen previously will be filtered out.
         *
         * @param facetsList
         * @return
         */
        private List<ScoredFacet> poolFacets(List<List<ScoredFacet>> facetsList) {
            HashSet<String> usedItems = new HashSet<>();
            ArrayList<ScoredFacet> pool = new ArrayList<>();
            ArrayList<Integer> listIndice = new ArrayList<>();
            for (int i = 0; i < facetsList.size(); i++) {
                listIndice.add(i);
            }

            for (int i = 0; i < facetTopNum; i++) {
                Collections.shuffle(listIndice);
                for (Integer index : listIndice) {
                    List<ScoredFacet> facets = facetsList.get(index);
                    if (i < facets.size()) {
                        ArrayList<ScoredItem> selectedItems = new ArrayList<>();
                        ScoredFacet facet = facets.get(i);
                        for (ScoredItem t : facet.items) {
                            if (!usedItems.contains(t.item)) {
                                selectedItems.add(t);
                                usedItems.add(t.item);
                            }
                        }
                        if (selectedItems.size() > 0) {
                            pool.add(new ScoredFacet(selectedItems, 0.0));
                        }
                    }
                }
            }

            return pool;
        }

    }

}
