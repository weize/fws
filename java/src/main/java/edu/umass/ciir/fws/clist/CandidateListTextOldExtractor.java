/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.crawl.Document;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Candidate list extract based on textual pattern. Takes in a document and
 * output a list of candidate lists based on HTML pattern.
 *
 * @author wkong
 */
public class CandidateListTextOldExtractor {

    public final static String type = "tx"; // text

    ArrayList<CandidateList> clists; // store the extracted lists
    String parseTreeText;
    String senText;
    TfQuery query;
    Document document;

    public CandidateListTextOldExtractor() {
        clists = new ArrayList<>();
    }

    public List<CandidateList> extract(Document document, TfQuery query, String parseFileContent) {
        this.clists.clear();
        this.query = query;
        this.document = document;

        String[] lines = parseFileContent.split("\n");
        for (int i = 0; i + 6 < lines.length; i += 7) {
            parseTreeText = lines[i];
            senText = lines[i + 1];
            if (containsAndOr(senText)) {
                try {
                    extractList(parseTreeText);
                } catch (Exception ex) {
                    System.err.println(ex);
                }
            }
        }
        return this.clists;
    }

    private void extractList(String parseTreeText) throws Exception {
        Node root;

        try {
            root = buildTreeNode(parseTreeText, 0);
        } catch (Exception e) {
            System.err.println(e);
            return;
        }
        ArrayList<Node> andOrNodes = new ArrayList<>();
        findAndOrNodes(root, andOrNodes);
        ArrayList<String> items = new ArrayList<>(); // candidate list items

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

            if (andOrNodeIdx == -1) {
                throw new Exception("Cannot find andOrNode node while processing:\n" + parseTreeText);
            }

            // find POS
            String pos = findKeyphrasePOSNearAndOrNode(children, andOrNodeIdx);
            if (pos.equals("X")) {
                continue;
            }

            // find valid keyphrase indices
            int begin = andOrNodeIdx;
            boolean lastIsKeyphrase = false; // valid keyphrases should be seperated by DelimiterNode
            for (; begin >= 0; begin--) {
                Node node = children[begin];
                if (lastIsKeyphrase) {
                    // should be sperated by a delimiter Node
                    if (isDelimiterNode(node)) {
                        lastIsKeyphrase = false;
                    } else {
                        break;
                    }
                } else {
                    if (isDelimiterNode(node)) {
                        lastIsKeyphrase = false;
                    } else if (POSEquivalent(node.pos, pos)) {
                        lastIsKeyphrase = true;
                    } else {
                        break;
                    }
                }
            }
            begin++;

            int end = andOrNodeIdx;
            lastIsKeyphrase = false; // valid keyphrases should be seperated by DelimiterNode
            for (; end < children.length; end++) {
                Node node = children[end];
                if (lastIsKeyphrase) {
                    // should be sperated by a delimiter Node
                    if (!isDelimiterNode(node)) {
                        break;
                    } else {
                        lastIsKeyphrase = false;
                    }
                } else {
                    if (isDelimiterNode(node)) {
                        lastIsKeyphrase = false;
                    } else if (POSEquivalent(node.pos, pos)) {
                        lastIsKeyphrase = true;
                    } else {
                        break;
                    }
                }
            }
            end--;

            // add keyphrases
            items.clear();
            for (int i = begin; i <= end; i++) {
                if (this.isDelimiterNode(children[i])) {
                    continue;
                }
                StringBuilder textBuilder = new StringBuilder();
                NodeText(children[i], textBuilder, used);
                String text = cleanText(textBuilder.toString());
                if (text.length() != 0) {
                    items.add(text);
                }
            }

            CandidateList clist = new CandidateList(query.id,
                    document.rank, document.name, type, items);
            if (clist.valid()) {
                clists.add(clist);
            }
        }

    }

    private void findAndOrNodes(Node cur, ArrayList<Node> andOrNodes) {
        if (cur.isLeaf) {
            if (isAndOrNode(cur)) {
                andOrNodes.add(cur);
            }
        } else {
            for (Node node : cur.children) {
                findAndOrNodes(node, andOrNodes);
            }
        }
    }

    private boolean isAndOrNode(Node cur) {
        if (!cur.isLeaf) {
            return false;
        }

        if (cur.text.equalsIgnoreCase("and") || cur.text.equalsIgnoreCase("or")) {
            return true;
        }
        return false;
    }

    private void NodeText(Node cur, StringBuilder text, HashSet<Node> used) {
        if (cur.isLeaf) {
            text.append(cur.text).append(" ");
        } else {
            for (Node node : cur.children) {
                NodeText(node, text, used);
            }
        }
    }

    private Node buildTreeNode(String parseTreeText, int start) throws Exception {
        int i = start + 1; // parseTreeText[start] should be (
        if (parseTreeText.charAt(start) != '(') {
            throw new Exception(
                    String.format("Qid %s\t document %s\n%s\nNode not starting with \"(\"",
                            query.id, document.name, parseTreeText));
        }
        Node cur = new Node();
        int wordEnd = firstBoundary(parseTreeText, i);
        String pos = parseTreeText.substring(i, wordEnd);
        cur.pos = pos;
        cur.start = start;

        i = wordEnd + 1;

        // text node
        if (parseTreeText.charAt(i) != '(') {
            // no chirdren, text node
            wordEnd = firstBoundary(parseTreeText, i);
            String text = parseTreeText.substring(i, wordEnd);
            cur.text = text;
            cur.isLeaf = true;
            cur.end = wordEnd;
            return cur;
        }

        // find chird
        ArrayList<Node> children = new ArrayList<Node>();
        while (true) {
            Node child = buildTreeNode(parseTreeText, i);
            child.father = cur;
            children.add(child);
            i = child.end + 1;

            // end
            if (parseTreeText.charAt(i) == ')') {
                cur.end = i;
                cur.children = children.toArray(new Node[0]);
                cur.isLeaf = false;
                break;
            }

            if (parseTreeText.charAt(i) != ' ') {
                throw new Exception(
                        String.format("Qid %s\t document %s\n%s\nNodes not seperated with space",
                                query.id, document.name, parseTreeText));
            }

            i++; // parseTreeText.charAt(i) should be a space
        }

        return cur;

    }

    private String cleanText(String text) {
        text = text.replace('\u00a0', ' ');
        text = text.replace('|', ' ');
        text = text.replaceAll("-LRB-", "(");
        text = text.replaceAll("-RRB-", ")");
        text = text.replaceAll("-LCB-", "{");
        text = text.replaceAll("-RCB-", "}");
        text = text.replaceAll("-LSB-", "[");
        text = text.replaceAll("-RSB-", "]");
        text = fixTokenizingDifference(text);
        text = text.replaceAll("\\s+", " ");

        return text.trim();
    }

    public static boolean containsAndOr(String text) {
        return TextProcessing.clean(text).matches(".*\\b(and|or)\\b.*");
    }

    private boolean isDelimiterNode(Node node) {
        if (this.isAndOrNode(node)) {
            return true;
        } else if (node.pos.equals(",")) {
            return true;
        } else {
            return false;
        }
    }

    private String findKeyphrasePOSNearAndOrNode(Node[] children, int andOrNodeIdx) {
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

    /**
     *
     * @param text
     * @return
     */
    private String fixTokenizingDifference(String text) {
        /**
         * Stanford core nlp handles single quotes differently to Galago (a)
         * Stanford: they're -> [they, re], (b) Galago: they're -> [theyre] This
         * function will convert (a) to (b)
         */
        text = text.replaceAll("\\s+'([\\p{L}\\p{N}])", "'$1");
        return text;
    }

    class Node {

        String pos;
        String text;
        int start;
        int end;
        boolean isLeaf = false;
        Node father;
        Node[] children;
    }

    private int firstBoundary(String text, int start) {
        int i = start;
        while (text.charAt(i) != ' ' && text.charAt(i) != '(' && text.charAt(i) != ')') {
            i++;
        }
        return i;
    }

}
