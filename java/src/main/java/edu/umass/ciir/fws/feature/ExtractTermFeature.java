package edu.umass.ciir.fws.feature;

import edu.umass.ciir.fws.tool.app.ProcessQueryApp;

/**
 * Tupleflow application that extract facet term features.
 *
 *
 * @author wkong
 */
public class ExtractTermFeature extends ProcessQueryApp {

    @Override
    protected Class getProcessClass() {
        return TermFeaturesExtractor.class;
    }

    @Override
    public String getName() {
        return "extract-term-feature";
    }
}
