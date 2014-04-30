/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.crawl.Document;
import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.TextTokenizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Candidate list extract based on textual pattern. Takes in a document and
 * output a list of candidate lists based on HTML pattern.
 *
 * @author wkong
 */
public class CandidateListTextExtractor {

    public final static String type = "tx"; // text

    ArrayList<CandidateList> clists; // store the extracted lists
    Query query;
    Document document;
    ParseTree tree;

    public CandidateListTextExtractor() {
        clists = new ArrayList<>();
    }

    public List<CandidateList> extract(Document document, Query query, String parseFileContent) {
        this.clists.clear();
        this.query = query;
        this.document = document;

        String[] lines = parseFileContent.split("\n");

        for (int i = 0; i + 3 < lines.length; i += 5) {
            String senText = lines[i];
            String treeText = lines[i + 1];
            String beginText = lines[i + 2];
            String endText = lines[i + 3];
            if (containsAndOr(senText)) {
                try {
                    // build parse tree
                    tree = new ParseTree(senText, treeText, beginText, endText);
                    extractCandidateListsFromParseTree();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.err.println(String.format("qid %s, docname %s: %s", query.id, document.name, ex.toString()));
                }
            }
        }
        return this.clists;
    }

    private void findAndOrNodes(Node node, ArrayList<Node> andOrNodes) {
        if (node.isLeaf) {
            if (node.isAndOrNode) {
                andOrNodes.add(node);
            }
        } else {
            for (Node child : node.children) {
                findAndOrNodes(child, andOrNodes);
            }
        }
    }

    public static boolean containsAndOr(String text) {
        return TextProcessing.clean(text).matches(".*\\b(and|or)\\b.*");
    }

    private boolean isDelimiterNode(Node node) {
        if (node.isAndOrNode) {
            return true;
        } else {
            return node.pos.equals(",");
        }
    }

    private String findPOSNearAndOrNode(Node[] children, int andOrNodeIdx) {
        for (int i = 1; andOrNodeIdx + i < children.length || andOrNodeIdx - i >= 0; i++) {
            if (andOrNodeIdx + i < children.length) {
                Node node = children[andOrNodeIdx + i];
                if (!isDelimiterNode(node)) {
                    return node.pos;
                }
            }

            if (andOrNodeIdx - i >= 0) {
                Node node = children[andOrNodeIdx - i];
                if (!isDelimiterNode(node)) {
                    return node.pos;
                }
            }
        }
        return "X";
    }

    private int findNodeInArray(Node[] nodes, Node cur) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].equals(cur)) {
                return i;
            }
        }
        return -1;
    }

    private boolean POSEquivalent(String pos1, String pos2) {
        //System.out.println(pos1 + "|" + pos2);
        if (pos1.length() <= 1 || pos2.length() <= 1) {
            if (pos1.length() == 1 && pos2.length() == 1) {
                return pos1.charAt(0) == pos2.charAt(0);
            } else {
                return false;
            }
        } else {
            return (pos1.charAt(0) == pos2.charAt(0)) && (pos1.charAt(1) == pos2.charAt(1));
        }

    }

    private void extractCandidateListsFromParseTree() {
        ArrayList<String> items = new ArrayList<>(); // candidate list items

        ArrayList<Node> andOrNodes = new ArrayList<>();
        findAndOrNodes(tree.root, andOrNodes);

        HashSet<Node> used = new HashSet<>();
        for (Node cur : andOrNodes) {
            Node father = cur.father;
            if (used.contains(father)) {
                continue;
            }

            used.add(father);
            Node[] children = father.children;

            // find andOrNode index
            int andOrNodeIdx = findNodeInArray(children, cur);

            // find POS
            String pos = findPOSNearAndOrNode(children, andOrNodeIdx);
            if (pos.equals("X")) {
                continue;
            }

            // find valid keyphrase indices
            int begin = andOrNodeIdx;
            boolean lastIsItem = false; // valid keyphrases should be seperated by DelimiterNode
            for (; begin >= 0; begin--) {
                Node node = children[begin];
                if (lastIsItem) {
                    // should be sperated by a delimiter Node
                    if (isDelimiterNode(node)) {
                        lastIsItem = false;
                    } else {
                        break;
                    }
                } else {
                    if (isDelimiterNode(node)) {
                        lastIsItem = false;
                    } else if (POSEquivalent(node.pos, pos)) {
                        lastIsItem = true;
                    } else {
                        break;
                    }
                }
            }
            begin++;

            int end = andOrNodeIdx;
            lastIsItem = false; // valid keyphrases should be seperated by DelimiterNode
            for (; end < children.length; end++) {
                Node node = children[end];
                if (lastIsItem) {
                    // should be sperated by a delimiter Node
                    if (!isDelimiterNode(node)) {
                        break;
                    } else {
                        lastIsItem = false;
                    }
                } else {
                    if (isDelimiterNode(node)) {
                        lastIsItem = false;
                    } else if (POSEquivalent(node.pos, pos)) {
                        lastIsItem = true;
                    } else {
                        break;
                    }
                }
            }
            end--;

            // add items
            items.clear();
            for (int i = begin; i <= end; i++) {
                if (this.isDelimiterNode(children[i])) {
                    continue;
                }
                String text = tree.getNodeExtendedText(children[i]);
                if (text.length() != 0) {
                    items.add(text);
                }
            }

            CandidateList clist = new CandidateList(query.id,
                    document.rank, type, items);
            if (clist.valid()) {
                clists.add(clist);
            }
        }

    }

    public static class Node {

        String text;
        String pos; // part of speach
        int tokenIndex; // index of the token. This corponds with begins and ends arrays in the parse tree.

        boolean isLeaf;
        boolean isAndOrNode;
        Node father;
        Node[] children;
    }

    public static class ParseTree {

        String senText; // original sentence
        String treeText; // parse tree text, e.g. (ROOT (S (S (VP (VBN Born) (S (NP (N ...
        int[] begins; // begin postions of each tokens
        int[] ends; // end positions of each tokens;
        Node root; // root node of the prase tree

        int position;
        int tokenIndex;

        public ParseTree(String senText, String treeText, String beginText, String endText) throws Exception {
            this.senText = senText;
            this.treeText = treeText;
            this.begins = readIntArray(beginText);
            this.ends = readIntArray(endText);
            build();
        }

        // build parse tree
        private void build() throws Exception {
            position = 0;
            tokenIndex = 0;

            root = buildNode();
        }

        public void printTree() {
            printNode(root);
        }

        private void printNode(Node node) {
            if (node.isLeaf) {
                System.out.print(String.format("(%s %s)", node.pos, getNodeText(node)));
            } else {
                System.out.print('(' + node.pos);
                for (Node child : node.children) {
                    System.out.print(' ');
                    printNode(child);
                }
                System.out.print(')');
            }
        }

        private String getNodeText(Node node) {
            int[] range = findTextNodeRange(node);
            int begin = begins[range[0]];
            int end = ends[range[1]];
            return senText.substring(begin, end);
        }

        /**
         * Extend the node text that begins/ends with APOSTROPHE. Using extended
         * text is due to the tokenizing difference between Galago and
         * StanfordNLP. Galago: we'll -> {well}, StanfordNLP: we'll -> {we, ll}.
         *
         * @param node
         * @return
         */
        private String getNodeExtendedText(Node node) {
            int[] range = findTextNodeRange(node);
            int beginI = range[0];
            int endI = range[1];

            for (; beginI > 0; beginI--) {
                int curBegin = begins[beginI]; // beginning position of this token
                int preEnd = ends[beginI - 1]; // ending position of previous token

                if (curBegin != preEnd) { // two tokens are sperated by spaces
                    break;
                } else {
                    char c1 = senText.charAt(curBegin);
                    char c2 = senText.charAt(preEnd - 1);
                    if (!TextTokenizer.isApostrophe(c1) && !TextTokenizer.isApostrophe(c2)) {
                        break;
                    }
                }
            }

            for (; endI + 1 < ends.length; endI++) {
                int curEnd = ends[endI]; // ending position of this token
                int preBegin = begins[endI + 1]; // begining position of previous token

                if (curEnd != preBegin) { // two tokens are sperated by spaces
                    break;
                } else {
                    char c1 = senText.charAt(curEnd - 1);
                    char c2 = senText.charAt(preBegin);
                    if (!TextTokenizer.isApostrophe(c1) && !TextTokenizer.isApostrophe(c2)) {
                        break;
                    }
                }
            }

            return senText.substring(begins[beginI], ends[endI]);
        }

        private Node buildNode() throws Exception {
            if (curChar() != '(') {
                throw new Exception("Node not starts with '('");
            }
            position++; // skip beginning '('

            Node cur = new Node();

            // process part-of-speech for this node
            getPartOfSpeech(cur);
            position++; // skip a space after the pos tag

            // process content of the node (text or children)
            if (curChar() != '(') { // this is a text node
                buildTextNode(cur);
            } else { // children
                buildChildrenNodes(cur);
            }

            position++; // skip ending ')' for cur node

            return cur;
        }

        private void buildChildrenNodes(Node node) throws Exception {
            ArrayList<Node> children = new ArrayList<>();
            while (true) {
                Node child = buildNode();
                child.father = node;
                children.add(child);

                char c = curChar();
                if (c == ')') { // ending ')' for cur node
                    node.children = children.toArray(new Node[0]);
                    node.isLeaf = false;
                    break;
                } else if (c == ' ') { // space between children
                    position++;
                } else {
                    throw new Exception("Nodes not seperated with space");
                }
            }
        }

        private void buildTextNode(Node node) {
            // process but ignore the text, since we will use the text in the original sentence.
            node.text = fetchNextToken();
            String text = TextProcessing.clean(node.text);
            node.isAndOrNode = text.equals("and") || text.equals("or");
            node.tokenIndex = tokenIndex++; // corrpsonds to begins and ends array
            node.isLeaf = true;
        }

        private void getPartOfSpeech(Node node) {
            node.pos = fetchNextToken();
        }

        /**
         * Start from current position to fetch the token in the parse tree.
         * This will increase position to the next char of the token.
         *
         * @return
         */
        private String fetchNextToken() {
            int begin = position;
            while (position < treeText.length()) {
                char c = curChar();
                if (c == ' ' || c == '(' || c == ')') {
                    break;
                }
                position++;
            }

            return treeText.substring(begin, position);
        }

        private char curChar() {
            return treeText.charAt(position);
        }

        private int[] readIntArray(String text) {
            String[] strings = text.split("\t");
            int[] ints = new int[strings.length];
            for (int i = 0; i < ints.length; i++) {
                ints[i] = Integer.parseInt(strings[i]);
            }
            return ints;
        }

        /**
         * Find the begin and end node indices that represented by the node.
         *
         * @param node
         * @return
         */
        private int[] findTextNodeRange(Node node) {
            if (node.isLeaf) {
                return new int[]{node.tokenIndex, node.tokenIndex};
            } else {
                int min = this.senText.length();
                int max = -1;
                for (Node child : node.children) {
                    int[] range = findTextNodeRange(child);
                    min = Math.min(min, range[0]);
                    max = Math.max(max, range[1]);
                }
                return new int[]{min, max};
            }

        }
    }
}
