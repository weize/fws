/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.demo;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.types.TfQuery;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class FWSEngineFactory {

    public static FWSEngine instance(Parameters p) {
        if (p.getBoolean("fake")) {
            return new FWSEngine() {

                @Override
                public List<ScoredFacet> generateFacets(TfQuery query) {
                    ArrayList<ScoredFacet> facets = new ArrayList<>();
                    for (int i = 1; i <= 5; i++) {

                        List<ScoredItem> items = new ArrayList<>();
                        for (int j = 1; j <= 5; j++) {
                            items.add(new ScoredItem(String.format("f%d.%d", i, j), 1));
                        }
                        facets.add(new ScoredFacet(items, 6 - i));
                    }
                    return facets;
                }

                @Override
                public List<RankedDocument> search(TfQuery query) {
                    return new ArrayList<>();
                }

            };
        } else {
            return new LocalFWSEngine(p);
        }
    }

}
