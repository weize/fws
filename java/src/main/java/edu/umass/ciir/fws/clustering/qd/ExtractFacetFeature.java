package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.tool.app.ProcessQueryApp;

/**
 * Tupleflow application that extract facet features for QD.
 *
 *
 * @author wkong
 */
public class ExtractFacetFeature extends ProcessQueryApp {

    @Override
    protected Class getProcessClass() {
        return QdFacetFeaturesExtractor.class;
    }

    @Override
    public String getName() {
        return "extract-facet-feature";
    }
}
