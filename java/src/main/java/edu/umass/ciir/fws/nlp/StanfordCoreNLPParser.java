/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.nlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.umass.ciir.fws.clist.CandidateListTextExtractor;
import edu.umass.ciir.fws.utility.Utility;
import java.io.*;
import java.util.*;

/**
 * Use StanfordCoreNLP parser to parse text. It first splits document content
 * into sentences, because StanfordCoreNLP parser can not take all sentences in
 * a document because of memory issues. Then, it pass each sentence to
 * StanfordCoreNLP parser for parsing (and other annotation). Outputs positions
 * of the tokens in the original sentence.
 *
 * @author wkong
 */
public class StanfordCoreNLPParser {

    StanfordCoreNLP pipeline;
    StanfordCoreNLP pipelineSsplit;
    BufferedWriter writer;

    public StanfordCoreNLPParser() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, parse");
        props.put("pos.maxlen", 50);
        props.put("parse.maxlen", 50);
        props.put("ssplit.isOneSentence", true);
        pipeline = new StanfordCoreNLP(props);

        Properties propsSen = new Properties();
        propsSen.put("annotators", "tokenize, ssplit");
        propsSen.put("ssplit.newlineIsSentenceBreak", "always");

        pipelineSsplit = new StanfordCoreNLP(propsSen);
    }

    /**
     * Parse the given text and write down parsing results in the given file.
     *
     * @param text
     * @param outputFileName
     * @throws IOException
     */
    public void parse(String text, String outputFileName) throws IOException {
        // Need to first split sentence
        writer = Utility.getWriter(outputFileName);
        List<String> sentences = getAndOrSentences(text);
        for (String sen : sentences) {
            // only parse potential setences (sentences that contains "and" or "or"
            // writer.write("\nsentence:\n" + sen + "\n\n");
            prasePerSentence(sen);
        }
        writer.close();

    }

    /**
     * Parsing each sentence.
     *
     * @param text
     * @throws IOException
     */
    private void prasePerSentence(String text) throws IOException {
        Annotation annotationSplit = new Annotation(text);
        try {
            pipeline.annotate(annotationSplit);
        } catch (Exception e) {
            System.err.println("failed to parse, skip: " + text);
        }
        writeOutAnnotation(annotationSplit);
    }

    private void writeOutAnnotation(Annotation annotationSplit) throws IOException {
        List<CoreMap> sentences = annotationSplit.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            // original sentence
            String text = sentence.get(CoreAnnotations.TextAnnotation.class).replace('\n', ' ');
            writer.write(text + "\n");

            // parse tree
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            writer.write(tree.toString() + "\n");

            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

//            // tokens
//            for (int i = 0; i < tokens.size() - 1; i++) {
//                String word = tokens.get(i).get(CoreAnnotations.TextAnnotation.class);
//                writer.write(word + "\t");
//            }
//            writer.write(tokens.get(tokens.size() - 1).get(CoreAnnotations.TextAnnotation.class));
//            writer.write("\n");
//
//            // pos tags
//            for (int i = 0; i < tokens.size() - 1; i++) {
//                String pos = tokens.get(i).get(CoreAnnotations.PartOfSpeechAnnotation.class);
//                writer.write(pos + "\t");
//            }
//            writer.write(tokens.get(tokens.size() - 1).get(CoreAnnotations.PartOfSpeechAnnotation.class));
//            writer.write("\n");
            // begin positions in the original sentence of each tokens
            for (int i = 0; i < tokens.size() - 1; i++) {
                writer.write(tokens.get(i).beginPosition() + "\t");
            }
            writer.write(tokens.get(tokens.size() - 1).beginPosition() + "\n");

            // end positions in the original sentence of each tokens
            for (int i = 0; i < tokens.size() - 1; i++) {
                writer.write(tokens.get(i).endPosition() + "\t");
            }
            writer.write(tokens.get(tokens.size() - 1).endPosition() + "\n");

//            // Named entity
//            for (int i = 0; i < tokens.size() - 1; i++) {
//                String ne = tokens.get(i).get(CoreAnnotations.NamedEntityTagAnnotation.class);
//                writer.write(ne + "\t");
//            }
//            writer.write(tokens.get(tokens.size() - 1).get(CoreAnnotations.NamedEntityTagAnnotation.class));
//            writer.write("\n");
//
//            
//            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class);
//            Integer[] sources = new Integer[tokens.size()];
//            String[] rels = new String[tokens.size()];
//            for (SemanticGraphEdge edge : dependencies.edgeListSorted()) {
//                String rel = edge.getRelation().toString();
//                rel = rel.replaceAll("\\s+", "");
//                int source = edge.getSource().index() - 1;
//                int target = edge.getTarget().index() - 1;
//                sources[target] = source;
//                rels[target] = rel;
//            }
//
//            for (int i = 0; i < sources.length; i++) {
//                if (rels[i] == null) {
//                    rels[i] = "__";
//                    sources[i] = -1;
//                }
//            }
//
//            // heads
//            writer.write(TextProcessing.join(sources, "\t"));
//            writer.write("\n");
//
//            // dependency relations
//            writer.write(TextProcessing.join(rels, "\t"));
//            writer.write("\n");
            writer.write("\n");
        }

    }

    /**
     * Split text into sentences. Need to do this first, otherwise parser will
     * take all text to do parsing which will case memory issue
     *
     * @param text
     * @return
     */
    private List<String> splitSentences(String text) {
        Annotation document = new Annotation(text);

        pipelineSsplit.annotate(document);

        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        ArrayList<String> sentencesText = new ArrayList<>();
        for (CoreMap sentence : sentences) {
            String senText = sentence.get(CoreAnnotations.TextAnnotation.class).trim();
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

            // limits for the number of tokens in a sentence: 300
            // Longer sentence are ignored, because of memory issues
            if (tokens.size() <= 300) {
                if (senText.length() > 0) {
                    sentencesText.add(senText);
                }
            } else {
                System.err.println("Warning: sentence too long :" + senText);
            }
        }
        return sentencesText;
    }

    public List<String> getAndOrSentences(String text) {
        ArrayList<String> sentences = new ArrayList<>();
        for (String sentence : splitSentences(text)) {
            if (CandidateListTextExtractor.containsAndOr(sentence)) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }
}
