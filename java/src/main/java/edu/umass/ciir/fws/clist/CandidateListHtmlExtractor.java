/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.types.Query;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * Will be used in CandidateListExtractor (Tupleflow App)
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
    Query query;
    Document document;

    public CandidateListHtmlExtractor() {
        clists = new ArrayList<>();
    }

    public List<CandidateList> extract(Document document, Query query) {
        this.clists.clear();
        this.query = query;
        this.document = document;

        try {
            htmlDoc = Jsoup.parse(document.html, "UTF-8");
        } catch (Exception exception) {
            System.err.println("ERROR: cannot process document - " + document.name);
            return clists;
        }

        // extract from ul, ol, select tags
        String[] tags = {HtmlTag.UL, HtmlTag.OL, HtmlTag.SELECT};
        for (String tag : tags) {
            Elements es = htmlDoc.getElementsByTag(tag);
            for (Element e : es) {
                extractFromUOLSelect(e);
            }
        }

        // extract from table
        Elements es = htmlDoc.getElementsByTag(HtmlTag.TABLE);
        for (Element e : es) {
            extractFromTABLE(e);
        }
        return clists;
    }

    /**
     * extract peer list from this UL / OL / select element
     *
     * @param e
     */
    private void extractFromUOLSelect(Element e) {
        Elements children = e.children();
        String type = e.tagName().toLowerCase();
        ArrayList<String> items = new ArrayList<>();

        String childType = type.equals(HtmlTag.SELECT) ? HtmlTag.OPTION : HtmlTag.LI;

        for (Element child : children) {
            if (child.tagName().equalsIgnoreCase(childType)) {
                String text = cleanText(getHeadingText(child));
                if (text.length() == 0) {
                    continue;
                }
                items.add(text);
            }
        }

        addCandidateList(type, items.toArray(new String[0]));
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

    /**
     * *
     * removes "|", because it's delimiter for itemlist
     *
     * @param text
     * @return
     */
    private String cleanText(String text) {
        text = text.replace('\u00a0', ' ');
        text = text.replace('|', ' ');
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }

    /**
     * extract lists from table
     *
     * @param e
     */
    private void extractFromTABLE(Element e) {
        ArrayList<ArrayList<String>> table = new ArrayList<ArrayList<String>>();
        for (Element tr : e.getElementsByTag(HtmlTag.TR)) {
            // not: <table> <tr>
            // not: <table> <tbody> <tr>
            if (!(tr.parent().equals(e) || (tr.parent().parent().equals(e)
                    && tr.parent().tagName().equalsIgnoreCase(HtmlTag.TBODY)))) {
                continue;
            }

            ArrayList<String> row = new ArrayList<String>();
            for (Element td : tr.children()) {
                if (td.tagName().equalsIgnoreCase(HtmlTag.TD)) {
                    String text = cleanText(this.getHeadingText(td));
                    if (text.length() == 0) {
                        continue;
                    }
                    row.add(text);
                }
            }
            if (row.size() > 0) {
                table.add(row);
            }
        }

        // row
        int maxw = 0;
        for (ArrayList<String> row : table) {
            String type = HtmlTag.TR;
            addCandidateList(type, row.toArray(new String[0]));
            if (row.size() > maxw) {
                maxw = row.size();
            }
        }

        // col
        for (int i = 0; i < maxw; i++) {
            String type = HtmlTag.TD;
            ArrayList<String> items = new ArrayList<String>();

            for (ArrayList<String> row : table) {
                if (row.size() > i) {
                    items.add(row.get(i));
                }
            }

            addCandidateList(type, items.toArray(new String[0]));
        }
    }

    private void addCandidateList(String type, String[] items) {
        CandidateList clist = new CandidateList(query.id, document.rank, type,
                items);
        if (clist.valid()) {
            clists.add(clist);
        }
    }
}
