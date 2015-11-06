package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.crawl.RankedDocument;
import edu.umass.ciir.fws.crawl.QuerySetResults;
import edu.umass.ciir.fws.tool.app.ProcessQueryApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Extract candidate lists. Extract raw (not cleaned/filtered) candidate lists
 * based on HTML and textual pattern. Tupleflow jobs splits by queries.
 *
 * @author wkong
 */
public class ExtractCandidateLists extends ProcessQueryApp {


    @Override
    protected Class getProcessClass() {
        return CandidateListExtractor.class;
    }

    @Override
    public String getName() {
        return "extract-candidate-lists";
    }

    /**
     * Candidate lists extractor that will be called by Tupleflow jobs to
     * extract candidate lists.
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    public static class CandidateListExtractor implements Processor<TfQuery> {

        QuerySetResults querySetResults;
        CandidateListHtmlExtractor cListHtmlExtractor;
        CandidateListTextExtractor cListTextExtractor;
        String parseDir;
        String docDir;
        String clistDir;

        public CandidateListExtractor(TupleFlowParameters parameters) throws Exception {
            Parameters p = parameters.getJSON();
            docDir = p.getString("docDir");
            long topNum = p.getLong("topNum");
            String rankedListFile = p.getString("rankedListFile");
            querySetResults = new QuerySetResults(rankedListFile, topNum);
            cListHtmlExtractor = new CandidateListHtmlExtractor();
            cListTextExtractor = new CandidateListTextExtractor();
            parseDir = p.getString("parseDir");
            clistDir = p.getString("clistDir");

        }

        @Override
        public void process(TfQuery query) throws IOException {
            List<RankedDocument> documents = RankedDocument.loadDocumentsFromFiles(querySetResults.get(query.id), docDir, query.id);
            ArrayList<CandidateList> clists = new ArrayList<>();
            for (RankedDocument doc : documents) {
                // extract by html patterns
                String docFileName = Utility.getDocHtmlFileName(docDir, query.id, doc.name);
                System.err.println("Processing " + docFileName);
                clists.addAll(cListHtmlExtractor.extract(doc, query));
                System.err.println("Done processing " + docFileName);

                // extract text patterns
                String parseFileName = Utility.getParsedDocFileName(parseDir, query.id, doc.name);
                String parseFileContent = Utility.readFileToString(new File(parseFileName));
                System.err.println("Processing " + parseFileName);
                clists.addAll(cListTextExtractor.extract(parseFileContent, doc, query));
                System.err.println("Done processing " + parseFileName);
            }
            File outfile = new File(Utility.getCandidateListRawFileName(clistDir, query.id));
            Utility.createDirectoryForFile(outfile);
            CandidateList.output(clists, outfile);
            Utility.infoWritten(outfile);
        }

        @Override
        public void close() throws IOException {
        }
    }

}
