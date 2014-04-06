/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.nlp;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
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

    public StanfordCoreNLPParser(String propsFile) {
        Properties props = loadProperties(propsFile);
        pipeline = new StanfordCoreNLP(props);
        
        Properties propsSen = new Properties();
        props.put("annotators", "tokenize, ssplit");
        pipelineSsplit = new StanfordCoreNLP(props);
    }

    public void run(String fileList) throws IOException {
        String[] files = IOUtils.slurpFile(fileList).trim().split("\n");
        for (String line : files) {
            String[] f2 = line.split("\t");
            process(f2[0], f2[1]);
        }
    }

    /***
     * 
     * @param text
     * @param outputFileName 
     */
    public void parse(String text, String outputFileName) throws IOException {
        // Need to first split sentence
        writer = Utility.getWriter(outputFileName);
        String[] sentences = splitSentences(text);
        for(String sen : sentences) {
            writer.write(sen);
            writer.write("\n");
        }
        writer.close();

    }

    private void process(String infile, String outfile) throws IOException {
        String text = IOUtils.slurpFile(infile);

        System.out.println("processing " + infile);
        BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
        String[] lines = text.split("\n");
        //for (int i = 0;; i += splitMaxLn) {
        for (String textSplit : lines) {
            //String textSplit = BuildSplit(lines, i, splitMaxLn);
            Annotation annotationSplit = new Annotation(textSplit);
            try {
                pipeline.annotate(annotationSplit);
            } catch (Exception e) {
                System.err.println("failed to parse, skip: " + textSplit);
                continue;
            }
//            if (i + splitMaxLn > lines.length) {
//                break;
//            }
            writeOutAnnotation(annotationSplit, out);

        }
        out.close();
        System.out.println("Output in " + outfile);
    }
    
    private void writeOutAnnotation(Annotation annotationSplit, BufferedWriter out) throws IOException {
        List<CoreMap> sentences = annotationSplit.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            out.write(tree.toString());
            out.write("\n");

            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class);

            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

            for (int i = 0; i < tokens.size() - 1; i++) {
                String word = tokens.get(i).get(CoreAnnotations.TextAnnotation.class);
                out.write(word + "\t");
            }
            out.write(tokens.get(tokens.size() - 1).get(CoreAnnotations.TextAnnotation.class));
            out.write("\n");

            for (int i = 0; i < tokens.size() - 1; i++) {
                String pos = tokens.get(i).get(CoreAnnotations.PartOfSpeechAnnotation.class);
                out.write(pos + "\t");
            }
            out.write(tokens.get(tokens.size() - 1).get(CoreAnnotations.PartOfSpeechAnnotation.class));
            out.write("\n");

            for (int i = 0; i < tokens.size() - 1; i++) {
                String ne = tokens.get(i).get(CoreAnnotations.NamedEntityTagAnnotation.class);
                out.write(ne + "\t");
            }
            out.write(tokens.get(tokens.size() - 1).get(CoreAnnotations.NamedEntityTagAnnotation.class));
            out.write("\n");

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

            out.write(TextProcessing.join(sources, "\t"));
            out.write("\n");

            out.write(TextProcessing.join(rels, "\t"));
            out.write("\n");
            out.write("\n");
        }

    }

    private Properties loadProperties(String propsFile) {
        Properties result = new Properties();
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(propsFile));
            result.load(is);
            // trim all values
            for (Object propKey : result.keySet()) {
                String newVal = result.getProperty((String) propKey);
                result.setProperty((String) propKey, newVal.trim());
            }
            is.close();
        } catch (IOException e) {
            System.err.println("argsToProperties could not read properties file: " + propsFile);
            throw new RuntimeException(e);
        }
        return result;
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
        ArrayList<String> words = new ArrayList<>();
        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            words.clear();
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(TextAnnotation.class);
                words.add(word);
            }
            sentencesText.add(TextProcessing.join(words, " "));
        }
        return sentencesText.toArray(new String[0]);
    }
}
