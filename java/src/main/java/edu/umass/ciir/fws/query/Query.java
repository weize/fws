package edu.umass.ciir.fws.query;

import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by wkong on 4/1/14.
 */
public class Query {

    public String id;
    public String text; // not cleaned

    public Query(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public Query(String line) {
        String[] fields = line.split("\t");
        this.id = fields[0];
        this.text = fields[1];
    }

    public static Query[] loadQueryList(String queryFile) throws IOException {
        ArrayList<Query> queries = new ArrayList<>();

        BufferedReader reader = Utility.getReader(queryFile);
        String line;
        while ((line = reader.readLine()) != null) {
            Query q = new Query(line);
            queries.add(q);
        }
        reader.close();

        return queries.toArray(new Query[0]);
    }
    
    public static String toSDM(Query q) {
        String textClean = TextProcessing.clean(q.text);
        return "#sdm( " + textClean + " )";
    }
}
