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

/**
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
        "progress", "q", "video","span"};

    public static String extract(String filename) throws IOException {
        File input = new File(filename);
        Document doc = Jsoup.parse(input, "UTF-8");

        StringBuilder text = new StringBuilder();
        Node node = doc;

        getNodeText(node, text);
        String text2;
        text2 = text.toString().trim().replaceAll("\\p{Z}", " ");

        // remove empty lines, and trim all lines
        text2 = text2.toString().replaceAll("\\s*\\n\\s*", "\n");

        // remove extra spacing
        text2 = text2.replaceAll("[\\s&&[^\\n]]+", " ");
        return text2;
    }

    private static void getNodeText(Node node, StringBuilder text) {

        if (node instanceof Element) {
            Element elementNode = (Element) node;
            String tagname = elementNode.tagName().toLowerCase();
            if (tagname.equals("script") || tagname.equals("noscript")
                    || tagname.equals("noframes") || tagname.equals("rp")) {
                return;
            }
        }

        if (node instanceof TextNode) {
            TextNode textNode = (TextNode) node;
            text.append(textNode.getWholeText());
            return;
        }

        for (Node node2 : node.childNodes()) {
            getNodeText(node2, text);
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
    }

    private static boolean needSpaceTag(String tagName) {
        tagName = tagName.toLowerCase();
        for (String tag : spaceTags) {
            if (tagName.equals(tag)) {
                return true;
            }
        }
        return false;
    }

    public void run(String filename) throws IOException {
        System.out.println(extract(filename));
    }

    private static boolean needNewLineTag(String tagName) {
        tagName = tagName.toLowerCase();
        for (String tag : newLineTags) {
            if (tagName.equals(tag)) {
                return true;
            }
        }
        return false;
    }
}
