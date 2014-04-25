/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.nlp;

import java.io.File;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * Extract text content from HTML.
 *
 * @author wkong
 */
public class HtmlContentExtractor {

    final static String[] newLineTags = {
        "article", "aside", "blockquote", "br", "caption",
        "div", "title", "h1", "h2", "h3", "h4", "h5", "h6",
        "hr", "iframe", "ol", "ul", "p", "table", "tr", "li",
        "option", "center", "dt", "dd", "details", "dir", "dl",
        "fieldset", "figcaption", "footer", "form", "frame", "frameset",
        "header", "hgroup", "iframe", "legend", "menu", "nav", "optgroup",
        "pre", "section", "select", "summary", "table", "textarea", "tfoot",
        "thead"
    };

    final static String[] spaceTags = {
        "audio", "button", "canvas", "caption", "th", "td",
        "img", "input", "embed", "figure", "keygen", "map", "object",
        "progress", "q", "video", "span"};
    
    final static String[] skippingTags = {"script", "noscript", "noframes", "rp"};

    public static String extractFromFile(String filename) throws IOException {
        File input = new File(filename);
        Document doc = Jsoup.parse(input, "UTF-8");
        return extract(doc);
    }

    public static String extractFromContent(String content) {
        Document doc = Jsoup.parse(content, "UTF-8");
        return extract(doc);
    }

    private static String extract(Document doc) {
        StringBuilder text = new StringBuilder();
        Node node = doc;

        getNodeText(node, text);
        String text2;
        text2 = text.toString().trim().replaceAll("\\p{Z}", " ");

        // remove empty lines, and trim all lines
        text2 = text2.replaceAll("\\s*\\n\\s*", "\n");

        // remove extra spacing
        text2 = text2.replaceAll("[\\s&&[^\\n]]+", " ");
        return text2;
    }

    /**
     * Extract the title.
     *
     * @param html
     * @return
     */
    public static String extractTitle(String html) {
        Document doc = Jsoup.parse(html, "UTF-8");
        Elements elems = doc.getElementsByTag("title");
        StringBuilder title = new StringBuilder();
        if (elems.size() > 0) {
            getNodeText(elems.get(0), title);
        }
        return title.toString();
    }

    public static void getNodeText(Node node, StringBuilder text) {

        if (node instanceof Element) {
            Element elementNode = (Element) node;
            if (isSkippingTag(elementNode.tagName())) {
                return;
            }
        }

        if (node instanceof TextNode) {
            TextNode textNode = (TextNode) node;
            text.append(textNode.getWholeText().replaceAll("\n", " "));
            return;
        }
        
        if (node instanceof Element) {
            Element elementNode = (Element) node;
            if (needNewLineTag(elementNode.tagName())) {
                text.append("\n");
            }

            if (needSpaceTag(elementNode.tagName())) {
                text.append(" ");
            }
        }

        for (Node node2 : node.childNodes()) {
            getNodeText(node2, text);
        }

        
    }

    private static boolean inTagNameSet(String tagName, String [] tagSet) {
        tagName = tagName.toLowerCase();
        for (String tag : tagSet) {
            if (tagName.equals(tag)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean needSpaceTag(String tagName) {
        return inTagNameSet(tagName, spaceTags);
    }

    public static boolean needNewLineTag(String tagName) {
        return inTagNameSet(tagName, newLineTags);
    }
    
     public static boolean isSkippingTag(String tagName) {
        return inTagNameSet(tagName, skippingTags);
    }

}
