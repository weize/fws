/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.umass.ciir.fws.nlp.HtmlContentExtractor;
import edu.umass.ciir.fws.nlp.PeerPatternNLPParser;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author wkong
 */
public class CandidateListNLPExtractor implements CandidateListExtractor {

    PeerPatternNLPParser nlpParser;
    CandidateListTextExtractor extractor;

    public CandidateListNLPExtractor() {
        nlpParser = new PeerPatternNLPParser();
        extractor = new CandidateListTextExtractor();
    }

    @Override
    public List<CandidateList> extract(List<RankedDocument> documents, TfQuery query) {
        List<CandidateList> clists = new ArrayList<>();
        for (RankedDocument doc : documents) {
            collect(doc, query, clists);
        }
        return clists;
    }

    private void collect(RankedDocument doc, TfQuery query, List<CandidateList> clists) {
        Utility.info(String.format("extract text candidate for rank=%d name=%s", doc.rank, doc.name));
        String content = HtmlContentExtractor.extractFromContent(doc.html);
        List<String> sentencesText = nlpParser.getAndOrSentences(content);
        for (String senText : sentencesText) {
            System.err.println("oooo: " + senText);
            Annotation annotationSplit = nlpParser.praseSentence(senText);
            List<CoreMap> sentences = annotationSplit.get(CoreAnnotations.SentencesAnnotation.class);
            for (CoreMap sentence : sentences) {
                clists.addAll(extractor.extract(sentence, doc, query));
            }
        }
    }

}
