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
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.types.TfQuery;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class LocalFacetGenerator implements FacetGenerator {

    String workingDir;
    SearchEngine searchEngine;
    CandidateListExtractor clistExtractor;
    CandidateListCleaner clistCleaner;
    FacetRefiner facetRefiner;

    int topDocs;

    public LocalFacetGenerator(Parameters p) {
        searchEngine = SearchEngineFactory.instance(p);
        clistExtractor = CandidateListExtractorFactory.instance(p);
        clistCleaner = new CandidateListCleaner(p);
        facetRefiner = FacetRefinerFactory.instance(p, searchEngine);
        topDocs = (int) p.getLong("topDocs");
    }

    @Override
    public List<ScoredFacet> generateFacets(TfQuery query) {
        List<RankedDocument> docs = searchEngine.getRankedDocuments(query, topDocs);
        List<CandidateList> clists = clistExtractor.extract(docs, query);
        clists = clistCleaner.clean(clists);
        List<ScoredFacet> facet = facetRefiner.refine(clists, docs);
        return facet;
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

}
