/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.retrieval.QuerySetResults;
import edu.umass.ciir.fws.feature.CluewebDocFreqMap;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Extract term features for each query.
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
public class QdFacetFeaturesTFExtractor extends QdFacetFeatureExtractor implements Processor<TfQuery> {

    CluewebDocFreqMap clueDfs;
    QuerySetResults querySetResults;

    TfQuery query;
    long topNum;
    String rankedListFile;
    String docDir;

    String clistDir;
    String qdFeatureDir;
    double clueCdf;

    public QdFacetFeaturesTFExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        clistDir = p.getString("clistDir");
         = Utility.getFileName(p.getString("qdRunDir"), "feature");
        String clueDfFile = p.getString("clueDfFile");
        clueCdf = p.getLong("clueCdf");
        topNum = p.getLong("topNum");
        rankedListFile = p.getString("rankedListFile");
        docDir = p.getString("docDir");
        clueDfs = new CluewebDocFreqMap(new File(clueDfFile), clueCdf);
        querySetResults = new QuerySetResults(rankedListFile, topNum);
    }

    @Override
    public void process(TfQuery query) throws IOException {
        this.query = query;

        System.err.println(String.format("processing query %s", query.id));

        List<CandidateList> clists = loadCandidateLists();
        List<RankedDocument> docs = loadDocuments();
        List<FacetFeatures> facetFeatures = extract(clists, docs);
        output(facetFeatures);
    }

    private List<CandidateList> loadCandidateLists() throws IOException {
        File clistFile = new File(Utility.getCandidateListFileName(clistDir, query.id, "clean.clist"));
        List<CandidateList> clists = CandidateList.loadCandidateLists(clistFile, topNum);
        return clists;
    }

    private List<RankedDocument> loadDocuments() throws IOException {
        return RankedDocument.loadDocumentsFromFiles(querySetResults.get(query.id), docDir, query.id);
    }

    private void output(List<FacetFeatures> facetFeatures) throws IOException {
        String fileName = Utility.getQdFacetFeatureFileName(qdFeatureDir, query.id);
        Writer writer = Utility.getWriter(fileName);
        for (FacetFeatures ff : facetFeatures) {
            writer.write(ff.toString());
            writer.write("\n");
        }
        writer.close();
        System.err.println(String.format("Written in %s", fileName));
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    protected double getDf(String term) {
        return clueDfs.getDf(term);
    }

    @Override
    protected double getCdf() {
        return clueCdf;
    }
}
