/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.nlp;

import edu.umass.ciir.fws.clist.CandidateListHtmlExtractor;
import java.io.File;
import java.io.IOException;
import java.util.Stack;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * Extract text content from HTML. Tries to preserve the original formatting of
 * the text, i.g. new lines, spaces.
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
        "pre", "section", "select", "summary", "textarea", "tfoot",
        "thead"
    };

    final static String[] spaceTags = {
        "audio", "button", "canvas", "caption", "th", "td",
        "img", "input", "embed", "figure", "keygen", "map", "object",
        "progress", "q", "video", "span"};

    final static String[] skippingTags = {"script", "noframes", "rp"};

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
        StringBuilder textBuilder = new StringBuilder();
        Node node = doc;

        getNodeText(node, textBuilder);
        String text = textBuilder.toString().trim().replaceAll("\\p{Z}", " ");
        text = text.replace('\u0092', '\u2019'); // correct invalid RIGHT SINGLE QUOTATION MARK 

        // remove empty lines, and trim all lines
        text = text.replaceAll("\\s*\\n\\s*", "\n");

        // remove extra spacing
        text = text.replaceAll("[\\s&&[^\\n]]+", " ");
        return text;
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
        if (node instanceof TextNode) {
            TextNode textNode = (TextNode) node;
            text.append(textNode.getWholeText().replaceAll("\n", " "));
        } else if (node instanceof Element) {
            Element elementNode = (Element) node;
            String spacing = "";
            if (isSkippingTag(elementNode.tagName())) {
                return; // skip tags such as <script>
            } else if (needNewLineTag(elementNode.tagName())) {
                spacing = "\n";
            } else if (needSpaceTag(elementNode.tagName())) {
                spacing = " ";
            }
            text.append(spacing);
            for (Node childNode : node.childNodes()) {
                getNodeText(childNode, text);
            }
            text.append(spacing);
        }
    }

    /**
     * Get text for list element, which may contain nested ol, ul elements. Only
     * get the first level text. e.g.
     * <li> A</li>
     * <li> 1</li>
     * <li> 2</li>
     * <li> B</li>
     * <li> C</li>
     * we'll extract {A, B, C}.
     *
     * @param root
     * @return
     */
    public static String getHeadingText(Element root) {
        StringBuilder text = new StringBuilder();
        Node node = root;
        int depth = 0;
        Stack<String> spacings = new Stack<>();

        /**
         * there are two types of node 1) element like ul, li, a, etc 2) text
         * node.
         */
        while (node != null) {
            String spacing = "";
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                text.append(textNode.text());
            } else if (node instanceof Element) {
                Element element = (Element) node;
                if (CandidateListHtmlExtractor.isListTag(element)) {
                    break;
                } else if (isSkippingTag(element.tagName())) {
                    node = node.nextSibling();
                    continue;
                } else if (needNewLineTag(element.tagName())) {
                    spacing = "\n";
                } else if (needSpaceTag(element.tagName())) {
                    spacing = " ";
                }
                text.append(spacing);
            }

            if (node.childNodes().size() > 0) {
                // get to the first child
                node = node.childNode(0);
                spacings.add(spacing);
                depth++;
            } else {
                while (node.nextSibling() == null && depth > 0) {
                    depth--;
                    text.append(spacings.pop());
                    node = node.parent();
                }
                if (node == root) {
                    break;
                }

                // get to the next sibling
                node = node.nextSibling();
            }
        }
        return text.toString();
    }

    private static boolean inTagNameSet(String tagName, String[] tagSet) {
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
