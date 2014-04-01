package edu.umass.ciir.fws.query;

import edu.umass.ciir.fws.utility.Utility;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Created by wkong on 4/1/14.
 */
public class GenerateQueryParameterFile extends AppFunction {
    @Override
    public String getName() {
        return "genQueryParamFile";
    }

    @Override
    public String getHelpString() {
        return "fws genQueryParamFile <parameters>+: \n"
                + "Parameters:\n"
                + "  --input={filename} : input query file. Format: <qid> TAB <text>\n"
                + "  --output={filename} \n"
                + "  --model={filename} \n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        assert (p.isString("input")) : "missing input file, --input";
        assert (p.isString("output")) : "missing output file, --output";
        assert (p.isString("model")) : "missing --model (SDM)";

        String inputFile = p.get("input", "");
        String outputFile = p.get("output", "");
        String model = p.get("model", "");

        Parameters queryParams = new Parameters();
        BufferedReader reader = Utility.getReader(inputFile);
        String line;
        
        queryParams.put("index", "this is the index");
        queryParams.put("query", "this is the query");
     
        ArrayList<Parameters> queries = new ArrayList<Parameters>();
        while((line = reader.readLine())!= null) {
            Query q = new Query(line);
            Parameters queryParam = new Parameters();
            queryParam.put("number", q.id);
            queryParam.put("text", q.text);
            queries.add(queryParam);
        }
        reader.close();
        
        queryParams.put("queries", queries);
        
        BufferedWriter writer = Utility.getWriter(outputFile);
        writer.write(queryParams.toPrettyString());
        writer.close();
       
    }
}
