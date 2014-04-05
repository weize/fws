/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import java.io.File;
import java.util.ArrayList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 *
 * @author wkong
 */
public class CandidateListHtmlExtractor {

    static class HtmlTag {

        final static String UL = "ul";
        final static String OL = "ol";
        final static String LI = "li";
        final static String SELECT = "select";
        final static String OPTION = "option";
        final static String TABLE = "table";
        final static String TR = "tr";
        final static String TD = "td";
        final static String TBODY = "tbody";

        public static boolean isListTag(Element e) {
            String tagname = e.tagName();
            tagname = tagname.toLowerCase();
            return (tagname.equals(OL) || tagname.equals(UL) || tagname.equals(SELECT) || tagname.equals(TABLE));
        }
    }
    ArrayList<CandidateList> clists;
    org.jsoup.nodes.Document htmlDoc;

    public CandidateListHtmlExtractor() {
        clists = new ArrayList<>();
    }

    private void extract(Document document) {
        clists.clear();

        try {
            htmlDoc = Jsoup.parse(document.html, "UTF-8");
        } catch (Exception ex) {
            System.err.println("ERROR: cannot process document - " + document.name);
            return;
        }


        // extract from ul
        Elements es;
        es = htmlDoc.getElementsByTag(HtmlTag.UL);
        for (Element e : es) {
            extractFromUOLSelect(e);
        }

        // extract from ol
        es = htmlDoc.getElementsByTag(HtmlTag.OL);
        for (Element e : es) {
            extractFromUOLSelect(e);
        }

        // extract from select
        es = htmlDoc.getElementsByTag(HtmlTag.SELECT);
        for (Element e : es) {
            extractFromUOLSelect(e);
        }

        // extract from table
        es = htmlDoc.getElementsByTag(HtmlTag.TABLE);
        for (Element e : es) {
            extractFromTABLE(e);
        }
    }

    /**
     * extract peer list from this UL / OL / select element
     *
     * @param e
     */
    private void extractFromUOLSelect(Element e) {
        Elements children = e.children();
        String type = e.tagName().toLowerCase();
        ArrayList<String> keyphrases = new ArrayList<>();

        String childType = type.equals(HtmlTag.SELECT) ? HtmlTag.OPTION : HtmlTag.LI;

        for (Element child : children) {
            if (child.tagName().equalsIgnoreCase(childType)) {
                String text = cleanText(getHeadingText(child));
                if (text.length() == 0) {
                    continue;
                }
                keyphrases.add(text);
            }
        }

        AspectHtmlList list = new AspectHtmlList(type, qid, docid, keyphrases.toArray(new String[0]));
        if (list.valid()) {
            lists.add(list);
        }
    }

    /**
     * get text for li element, which may contain nested ol, ul elements
     *
     * @param root
     * @return
     */
    private String getHeadingText(Element root) {
        StringBuilder text = new StringBuilder();
        Node node = root;
        int depth = 0;

        /**
         * there are two types of node 1) element like ul, li, a, etc 2) text
         * node
         */
        while (node != null) {

            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                text.append(' ');
                text.append(textNode.text());
            } else if (node instanceof Element) {
                Element element = (Element) node;
                if (HtmlTag.isListTag(element)) {
                    break;
                }
            }

            if (node.childNodes().size() > 0) {
                node = node.childNode(0);
                depth++;
            } else {
                while (node.nextSibling() == null && depth > 0) {
                    node = node.parent();
                    depth--;
                }
                if (node == root) {
                    break;
                }
                node = node.nextSibling();
            }
        }
        return text.toString();
    }
    
    
    private String cleanText(String text) {
        text = text.replace('\u00a0', ' ');
        text = text.replace('|', ' ');
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }
}
