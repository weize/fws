// BSD License (http://lemurproject.org/galago-license)
package edu.umass.ciir.fws.demo;

import edu.umass.ciir.fws.clist.CandidateListHtmlExtractor;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.types.TfQuery;
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
        writer.write("body { font-family: Helvetica, sans-serif; }\n");
        writer.write("img { border-style: none; }\n");
        writer.write("#box { border: 1px solid #ccc; margin: 100px auto; width: 500px;"
                + "background: rgb(210, 233, 217); }\n");
        writer.write("#box a { font-size: small; text-decoration: none; }\n");
        writer.write("#box a:link { color: rgb(0, 93, 40); }\n");
        writer.write("#box a:visited { color: rgb(90, 93, 90); }\n");
        writer.write("#header { background: rgb(210, 233, 217); border: 1px solid #ccc; }\n");
        writer.write("#result { padding: 10px 5px; max-width: 550px; }\n");
        writer.write("#meta { font-size: small; color: rgb(60, 100, 60); }\n");
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
        writer.append("<title>Galago Search</title></head>");
        writer.append("<body>");
        writer.append("<center><br/><br/><div>");
        writer.append("<form action=\"search\"><input name=\"q\" size=\"40\">"
                + "<input value=\"Search\" type=\"submit\" /></form><br/><br/>");
        writer.append("</div></center></body></html>\n");
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
        List<ScoredFacet> facetResult = performExtraction(request);
        
        response.setContentType("text/html");
        String displayQuery = scrub(request.getParameter("q"));
        String encodedQuery = URLEncoder.encode(request.getParameter("q"), "UTF-8");

        PrintWriter writer = response.getWriter();
        writer.append("<html>\n");
        writer.append("<head>\n");
        writer.append(String.format("<title>%s - Galago Search</title>\n", displayQuery));
        writeStyle(writer);
        writer.append("<script type=\"text/javascript\">\n");
        writer.append("function toggleDebug() {\n");
        writer.append("   var object = document.getElementById('debug');\n");
        writer.append("   if (object.style.display != 'block') {\n");
        writer.append("     object.style.display = 'block';\n");
        writer.append("  } else {\n");
        writer.append("     object.style.display = 'none';\n");
        writer.append("  }\n");
        writer.append("}\n");
        writer.append("</script>\n");
        writer.append("</head>\n<body>\n");
        writer.append(displayQuery);
        writer.append("<br/>");
        for(ScoredFacet facet : facetResult) {
            writer.append(facet.toFacetString());
            writer.append("<br/>");
        }
        writer.append("</body>");
        writer.append("</html>");
        writer.close();
    }

    private List<ScoredFacet> performExtraction(HttpServletRequest request) {
        String queryText = request.getParameter("q");
        String qid = getQueryId();
        TfQuery query = new TfQuery(qid, queryText);
        Logger.getLogger("runtime").log(Level.INFO, "q:"+query);
        Parameters p = new Parameters();
        List<ScoredFacet> result = demo.runExtraction(query, p);
        return result;
    }

    private String getQueryId() {
        return "1";
    }

}
