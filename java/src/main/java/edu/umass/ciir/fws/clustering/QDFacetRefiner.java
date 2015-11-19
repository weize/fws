/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clustering.qd.FacetFeatures;
import edu.umass.ciir.fws.clustering.qd.QdFacetFeatureOnlineExtractor;
import edu.umass.ciir.fws.clustering.qd.QueryDimensionClusterer;
import edu.umass.ciir.fws.demo.search.GalagoSearchEngine;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.utility.Utility;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class QDFacetRefiner implements FacetRefiner {

    QdFacetFeatureOnlineExtractor facetFeatureExtractor;
    QueryDimensionClusterer qdCluster;
    double itemRatio;

    public QDFacetRefiner(Parameters p, GalagoSearchEngine galago) {
        
        facetFeatureExtractor = new QdFacetFeatureOnlineExtractor(p, galago);
        
        qdCluster = new QueryDimensionClusterer(p);
    }

    @Override
    public List<ScoredFacet> refine(List<CandidateList> clists, List<RankedDocument> docs) {
        Utility.info("extract facet features...");
        List<FacetFeatures> nodes = facetFeatureExtractor.extract(clists, docs);
        Utility.info("cluster candiate lists...");
        return qdCluster.clusterToFacets(nodes);
    }
}
