// BSD License (http://lemurproject.org/galago-license)
package edu.umass.ciir.fws.demo;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.retrieval.RankedDocument;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.*;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.web.WebHandler;

/**
 *
 * @author wkong
 */
public class DemoWebHandler implements WebHandler {

    protected Demo demo;

    public DemoWebHandler(Demo demo) {
        this.demo = demo;
    }

    public void writeStyle(PrintWriter writer) {
        writer.write("<style type=\"text/css\">\n");
        writer.write("body { font-family: Helvetica, sans-serif; margin: 8px; padding: 0px; }\n");
        writer.write("img { border-style: none; }\n");
        writer.write("#box { border: 1px solid #ccc; margin: 100px auto; width: 500px;"
                + "background: rgb(210, 233, 217); }\n");
        writer.write("#box a { font-size: small; text-decoration: none; }\n");
        writer.write("#box a:link { color: rgb(0, 93, 40); }\n");
        writer.write("#box a:visited { color: rgb(90, 93, 90); }\n");
        writer.write("#header { background: rgb(210, 233, 217); border: 1px solid #ccc; }\n");
        writer.write("#result { padding: 10px 5px; max-width: 550px; }\n");
        writer.write("#meta { font-size: small; color: rgb(60, 100, 60); }\n");
        writer.write(".fterm {  }\n");
        writer.write(".message { position: absolute;  color:green; border: solid 1px yellowgreen; margin-left: 40px; margin-top: 10px; padding: 2px}\n");
        writer.write(".search { position: absolute; left: 270px; top:70px }\n");
        writer.write(".dlist {position: absolute; left: 270px; top:95px }\n");
        writer.write(".flist {position: absolute; width: 250px; top:55px}\n");
        writer.write("#summary { font-size: small; }\n");
        writer.write("#debug { display: none; }\n");
        writer.write("</style>");
    }

    public void handleMainPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        response.setContentType("text/html");

        writer.append("<html>\n");
        writer.append("<head>\n");
        writeStyle(writer);
        writer.append("<title>Faceted Web Search</title></head>");
        writer.append("<body>");
        writeHeadMessage(writer);
        writer.append("<div class=\"search\">");
        writer.append("<form action=\"search\"><input name=\"q\" size=\"40\">"
                + "<input value=\"Search\" type=\"submit\" /></form><br/><br/>");
        writer.append("</div></body></html>\n");
        writer.close();
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getPathInfo().equals("/search")) {
            try {
                handleSearch(request, response);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServletException("Caught exception from handleSearch", e);
            }
        } else {
            handleMainPage(request, response);
        }
    }

    protected String scrub(String s) throws UnsupportedEncodingException {
        if (s == null) {
            return null;
        }
        return s.replace("<", "&gt;").replace(">", "&lt;").replace("&", "&amp;");
    }

    public void handleSearch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        TfQuery query = extractQuery(request);
        List<RankedDocument> docs = performSearch(query, request);
        List<ScoredFacet> facets = performExtraction(query, request);

        response.setContentType("text/html");
        String displayQuery = scrub(query.text);
        String encodedQuery = URLEncoder.encode(request.getParameter("q"), "UTF-8");

        PrintWriter writer = response.getWriter();

        writer.append("<html>\n");
        writer.append("<head>\n");
        writeStyle(writer);
        writer.append("<title>Faceted Web Search</title></head>");
        writer.append("<body>");
        writeHeadMessage(writer);
        writer.append("<div class=\"search\">");
        writer.append("<form action=\"search\">");
        // query
        writer.append(String.format("<input name=\"q\" size=\"40\" value=\"%s\">", displayQuery));
        writer.append("<input value=\"Search\" type=\"submit\" /></form><br/><br/>");
        writer.append("</div>");

        // facets
        writer.append("<div class=\"flist\"><ul>");
        for (ScoredFacet facet : facets) {

            writer.append("<li>");
            writer.append("<ul>");
            for (ScoredItem item : facet.items) {
                writer.append("<li>");
                writer.append("<div class=\"fterm\">");
                writer.append(item.item);
                writer.append("</div>");
                writer.append("</li>");
            }
            writer.append("</ul>");
            writer.append("</li>");
        }
        writer.append("<ul></div>");

        // web results
        writer.append("<div class=\"dlist\">");

        for (RankedDocument d : docs) {
            writer.append("<div id=\"result\">\n");
            writer.append(String.format("<a href=\"%s\">%s</a><br/>"
                    + "<div id=\"summary\">%s</div>\n"
                    + "<div id=\"meta\">%s</div>\n",
                    d.url,
                    d.titleRaw,
                    "",
                    scrub(d.url)));
            writer.append("</div>\n");
        }
        writer.append("</body></html>\n");
        writer.append("</div>");
        writer.close();
    }

    private List<ScoredFacet> performExtraction(TfQuery query, HttpServletRequest request) {
        Parameters p = new Parameters();
        List<ScoredFacet> result = demo.runExtraction(query, p);
        return result;
    }

    private String getQueryId() {
        return "1";
    }

    private List<RankedDocument> performSearch(TfQuery query, HttpServletRequest request) {

        Parameters p = new Parameters();
        List<RankedDocument> result = demo.runSearch(query, p);
        return result;
    }

    private TfQuery extractQuery(HttpServletRequest request) {
        String queryText = request.getParameter("q");
        String qid = getQueryId();
        TfQuery query = new TfQuery(qid, queryText);
        Utility.info("q:" + query);
        return query;
    }

    private void writeHeadMessage(PrintWriter writer) {
        writer.append("<div class=\"message\"> Please be patient. The system need some to download webpages for producing amazing results.</div>");
        
    }

}
