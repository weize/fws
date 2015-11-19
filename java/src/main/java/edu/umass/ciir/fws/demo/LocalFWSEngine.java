/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.demo;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clist.CandidateListCleaner;
import edu.umass.ciir.fws.clist.CandidateListExtractor;
import edu.umass.ciir.fws.clist.CandidateListExtractorFactory;
import edu.umass.ciir.fws.clustering.FacetRefiner;
import edu.umass.ciir.fws.clustering.FacetRefinerFactory;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.demo.search.SearchEngine;
import edu.umass.ciir.fws.demo.search.SearchEngineFactory;
import edu.umass.ciir.fws.feature.CandidateListDocFreqMap;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class LocalFWSEngine implements FWSEngine {

    String workingDir;
    SearchEngine searchEngine;
    CandidateListExtractor clistExtractor;
    CandidateListCleaner clistCleaner;
    FacetRefiner facetRefiner;
    List<RankedDocument> docs; // save search results
    String curQuery;

    int topDocs;

    public LocalFWSEngine(Parameters p) {
        searchEngine = SearchEngineFactory.instance(p);
        clistExtractor = CandidateListExtractorFactory.instance(p);
        clistCleaner = new CandidateListCleaner(p);
        facetRefiner = FacetRefinerFactory.instance(p, searchEngine);
        topDocs = (int) p.getLong("topDocs");
    }

    @Override
    public List<ScoredFacet> generateFacets(TfQuery query) {
        if (isNewQuery(query)) {
            search(query);
        }
        Utility.info("extract candidate lists...");
        List<CandidateList> clists = clistExtractor.extract(docs, query);
        clists = clistCleaner.clean(clists);
        Utility.info("#candiateLists=" + clists.size());
        List<ScoredFacet> facet = facetRefiner.refine(clists, docs);
        Utility.info("done");
        return facet;
    }

    @Override
    public List<RankedDocument> search(TfQuery query) {
        Utility.info("run search");
        if (isNewQuery(query)) {
            docs = searchEngine.getRankedDocuments(query, topDocs);
            curQuery = query.text;
        }
        return docs;
    }

    public List<ScoredFacet> generateFacetsFake(List<CandidateList> clists) {
        int idx = 0;
        ArrayList<ScoredFacet> facets = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            List<ScoredItem> items = new ArrayList<>();
            CandidateList clist = clists.get(idx);
            idx = Math.min(idx + 1, clists.size() - 1);
            int idx2 = 0;
            for (int j = 1; j <= 5; j++) {
                String term = clist.items[idx2];
                idx2 = Math.min(idx2 + 1, clist.items.length - 1);
                items.add(new ScoredItem(term, 1));
            }
            facets.add(new ScoredFacet(items, 6 - i));
        }
        return facets;
    }

    private boolean isNewQuery(TfQuery query) {
        return curQuery == null ? true : !curQuery.equals(query.text);
    }

}
