/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.retrieval;

import java.io.IOException;
import org.lemurproject.galago.core.retrieval.iterator.CountIterator;
import org.lemurproject.galago.core.retrieval.iterator.IndicatorIterator;
import org.lemurproject.galago.core.retrieval.iterator.TransformIterator;
import org.lemurproject.galago.core.retrieval.processing.ScoringContext;
import org.lemurproject.galago.core.retrieval.query.AnnotatedNode;
import org.lemurproject.galago.core.retrieval.query.NodeParameters;

/**
 *
 * @author wkong
 */
public class ExistIterator extends TransformIterator implements IndicatorIterator {

    private final CountIterator counts;
    private ScoringContext fakeScoringContext;

    public ExistIterator(NodeParameters np, CountIterator counts) throws IOException {
        super(counts);
        this.counts = counts;
        this.fakeScoringContext = new ScoringContext();
    }

    @Override
    public boolean indicator(ScoringContext c) {
        return counts.count(c) > 0;
    }

    @Override
    public boolean hasAllCandidates() {
        return counts.hasAllCandidates();
    }

    @Override
    public boolean hasMatch(long id) {
        if (!counts.hasMatch(id)) {
            return false;
        }
        fakeScoringContext.document = id;
        return indicator(fakeScoringContext);
    }

    @Override
    public AnnotatedNode getAnnotatedNode(ScoringContext sc) throws IOException {
        return null;
    }
}
