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
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.*;
import java.util.*;

/**
 *
 * @author wkong
 */
public class StanfordCoreNLPParser {

    StanfordCoreNLP pipeline;
    StanfordCoreNLP pipelineSsplit;
    int splitMaxLn = 100;
    BufferedWriter writer;

    public StanfordCoreNLPParser() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
        props.put("parse.maxlen", 80);
        props.put("ssplit.isOneSentence", true);
        pipeline = new StanfordCoreNLP(props);

        Properties propsSen = new Properties();
        propsSen.put("annotators", "tokenize, ssplit");
        propsSen.put("ssplit.newlineIsSentenceBreak", "always");

        pipelineSsplit = new StanfordCoreNLP(propsSen);
    }
    
    /**
     * *
     *
     * @param text
     * @param outputFileName
     */
    public void parse(String text, String outputFileName) throws IOException {
        // Need to first split sentence
        writer = Utility.getWriter(outputFileName);
        String[] sentences = splitSentences(text);
        for (String sen : sentences) {
            // only parse potential setences (sentences that contains "and" or "or"
            // writer.write(sen+"\n\n");
            if (containAndOr(sen)) {
                prasePerSentence(sen);
            }
        }
        writer.close();

    }

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
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            writer.write(tree.toString());
            writer.write("\n");

            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class);

            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

            for (int i = 0; i < tokens.size() - 1; i++) {
                String word = tokens.get(i).get(CoreAnnotations.TextAnnotation.class);
                writer.write(word + "\t");
            }
            writer.write(tokens.get(tokens.size() - 1).get(CoreAnnotations.TextAnnotation.class));
            writer.write("\n");

            for (int i = 0; i < tokens.size() - 1; i++) {
                String pos = tokens.get(i).get(CoreAnnotations.PartOfSpeechAnnotation.class);
                writer.write(pos + "\t");
            }
            writer.write(tokens.get(tokens.size() - 1).get(CoreAnnotations.PartOfSpeechAnnotation.class));
            writer.write("\n");

            for (int i = 0; i < tokens.size() - 1; i++) {
                String ne = tokens.get(i).get(CoreAnnotations.NamedEntityTagAnnotation.class);
                writer.write(ne + "\t");
            }
            writer.write(tokens.get(tokens.size() - 1).get(CoreAnnotations.NamedEntityTagAnnotation.class));
            writer.write("\n");

            Integer[] sources = new Integer[tokens.size()];
            String[] rels = new String[tokens.size()];
            for (SemanticGraphEdge edge : dependencies.edgeListSorted()) {
                String rel = edge.getRelation().toString();
                rel = rel.replaceAll("\\s+", "");
                int source = edge.getSource().index() - 1;
                int target = edge.getTarget().index() - 1;
                sources[target] = source;
                rels[target] = rel;
            }

            for (int i = 0; i < sources.length; i++) {
                if (rels[i] == null) {
                    rels[i] = "__";
                    sources[i] = -1;
                }
            }

            writer.write(TextProcessing.join(sources, "\t"));
            writer.write("\n");

            writer.write(TextProcessing.join(rels, "\t"));
            writer.write("\n");
            writer.write("\n");
        }

    }

    /**
     * Split text into sentence Need to do this first, otherwise parser will
     * take all text to do parsing which will case memory issue
     *
     * @param text
     * @return
     */
    private String[] splitSentences(String text) {
        Annotation document = new Annotation(text);

        pipelineSsplit.annotate(document);

        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        ArrayList<String> sentencesText = new ArrayList<>();
        for (CoreMap sentence : sentences) {
            String senText = sentence.get(CoreAnnotations.TextAnnotation.class).trim();
            if (senText.length() > 0) {
                sentencesText.add(senText);
            }
        }
        return sentencesText.toArray(new String[0]);
    }

    private boolean containAndOr(String text) {
        return text.replaceAll("\n+", " ").toLowerCase().matches(".*\\b(and|or)\\b.*");
    }
}