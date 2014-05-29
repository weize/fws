/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.query;

import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQuery")
public class QueryFileParser extends StandardStep<FileName, TfQuery> {

    @Override
    public void process(FileName fileName) throws IOException {
        TfQuery[] queries = QueryFileParser.loadQueryList(fileName.filename);
        for (TfQuery q : queries) {
            processor.process(q);
        }
    }

    public static TfQuery[] loadQueryList(File inputFile) throws IOException {
        BufferedReader reader = Utility.getReader(inputFile);
        String line;
        ArrayList<TfQuery> queries = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            String qid = fields[0];
            String text = fields[1];
            queries.add(new TfQuery(qid, text));

        }
        reader.close();
        return queries.toArray(new TfQuery[0]);
    }

    public static TfQuery[] loadQueryList(String fileName) throws IOException {
        return loadQueryList(new File(fileName));
    }

    public static HashMap<String, TfQuery> loadQueryMap(File file) throws IOException {
        TfQuery[] queries = loadQueryList(file);
        HashMap<String, TfQuery> map = new HashMap<>();
        for (TfQuery q : queries) {
            map.put(q.id, q);
        }
        return map;
    }

    public static void output(TfQuery[] queries, String filename) throws IOException {
        BufferedWriter writer = Utility.getWriter(filename);
        for (TfQuery query : queries) {
            writer.write(String.format("%s\t%s\n", query.id, query.text));
        }
        writer.close();
    }

}
