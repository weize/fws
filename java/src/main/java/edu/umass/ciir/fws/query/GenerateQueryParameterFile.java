package edu.umass.ciir.fws.query;

import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Created by wkong on 4/1/14.
 */
public class GenerateQueryParameterFile extends AppFunction {
    private static final String name = "generate-query-params";
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHelpString() {
        return "fws " + name + " <parameters>+: \n"
                + "Parameters:\n"
                + "  --queryFile={filename} : input query file. Format: <qid> TAB <text>\n"
                + "  --output={filename} \n"
                + "  --model={filename} \n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        assert (p.isString("queryFile")) : "missing input file, --input";
        assert (p.isString("output")) : "missing output file, --output";
        assert (p.isString("model")) : "missing --model (SDM)";

        String inputFile = p.get("queryFile", "");
        String outputFile = p.get("output", "");
        String model = p.get("model", "");

        Parameters queryParams = new Parameters();
        
        
        queryParams.put("index", p.get("index"));
        queryParams.put("requested", p.get("requested"));
        
        TfQuery [] queries = QueryFileParser.loadQueryList(inputFile);
        ArrayList<Parameters> queriesParam = new ArrayList<Parameters>();
        for (TfQuery q : queries) {
            Parameters queryParam = new Parameters();
            
            TfQuery newQuery = q;
            if (model.equalsIgnoreCase("sdm")) {
                newQuery = QueryProcessing.toSDMQuery(q);
            } else if (model.equalsIgnoreCase("rm")){
                //newQuery = QueryProcessing.toRMSdm(q);
                
            }
            queryParam.put("number", newQuery.id);
            queryParam.put("text", newQuery.text);
            queriesParam.add(queryParam);
        }
        queryParams.put("queries", queriesParam);
        
        BufferedWriter writer = Utility.getWriter(outputFile);
        writer.write(queryParams.toPrettyString());
        writer.newLine();
        writer.close();
        output.println("written in " + outputFile);
       
    }
}
