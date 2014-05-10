/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.crawl.Document;
import edu.umass.ciir.fws.nlp.HtmlContentExtractor;
import edu.umass.ciir.fws.types.TfQuery;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Candidate list extract based on HTML pattern. Takes in a document and output
 * a list of candidate lists based on HTML pattern.
 *
 * @author wkong
 */
public class CandidateListHtmlExtractor {

    public static class HtmlTag {

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
    TfQuery query;
    Document document;

    public CandidateListHtmlExtractor() {
        clists = new ArrayList<>();
    }

    public List<CandidateList> extract(String docHtml, long docRank, String docName, String queryID) {
        Document doc = new Document();
        doc.html = docHtml;
        doc.rank = docRank;
        doc.name = docName;
        TfQuery q = new TfQuery(queryID, "");
        return extract(doc, q);
    }

    public List<CandidateList> extract(Document document, TfQuery query) {
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
            if (!isDescendantOfSkippedElement(es)) {
                for (Element e : es) {
                    extractFromUOLSelect(e);
                }
            }
        }

        // extract from table
        Elements es = htmlDoc.getElementsByTag(HtmlTag.TABLE);
        for (Element e : es) {
            if (!isDescendantOfSkippedElement(es)) {
                extractFromTABLE(e);
            }
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
                String text = cleanText(HtmlContentExtractor.getHeadingText(child));
                if (text.length() == 0) {
                    continue;
                }
                items.add(text);
            }
        }

        addCandidateList(type, items);
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
        text = text.replace('\u0092', '\'');
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
        ArrayList<ArrayList<String>> table = new ArrayList<>();
        for (Element tr : e.getElementsByTag(HtmlTag.TR)) {
            // not: <table> <tr>
            // not: <table> <tbody> <tr>
            if (!(tr.parent().equals(e) || (tr.parent().parent().equals(e)
                    && tr.parent().tagName().equalsIgnoreCase(HtmlTag.TBODY)))) {
                continue;
            }

            ArrayList<String> row = new ArrayList<>();
            for (Element td : tr.children()) {
                if (td.tagName().equalsIgnoreCase(HtmlTag.TD)) {
                    String text = cleanText(HtmlContentExtractor.getHeadingText(td));
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

        // row-wise
        int maxw = 0;
        for (ArrayList<String> row : table) {
            String type = HtmlTag.TR;
            addCandidateList(type, row);
            if (row.size() > maxw) {
                maxw = row.size();
            }
        }

        // col-wise
        for (int i = 0; i < maxw; i++) {
            String type = HtmlTag.TD;
            ArrayList<String> items = new ArrayList<>();

            for (ArrayList<String> row : table) {
                if (row.size() > i) {
                    items.add(row.get(i));
                }
            }

            addCandidateList(type, items);
        }
    }

    private void addCandidateList(String type, List<String> items) {
        CandidateList clist = new CandidateList(query.id, document.rank, document.name, type,
                items);
        if (clist.valid()) {
            clists.add(clist);
        }
    }

    private boolean isDescendantOfSkippedElement(Elements element) {
        for (Element e : element.parents()) {
            if (HtmlContentExtractor.isSkippingTag(e.tagName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isListTag(Element e) {
        return HtmlTag.isListTag(e);
    }
}
