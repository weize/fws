/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.crawl.Document;
import edu.umass.ciir.fws.crawl.QuerySetDocuments;
import edu.umass.ciir.fws.types.CandidateList;
import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.Query")
@OutputClass(className = "edu.umass.ciir.fws.types.CandidateList")
public class CandidateListParser extends StandardStep<Query, CandidateList> {

    String parseDir;

    public CandidateListParser(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        parseDir = p.getString("clistDir");
    }

    @Override
    public void process(Query query) throws IOException {
        String fileName = String.format("%s%s%s.clist", parseDir, File.separator,
                query.id);
        String[] lines = Utility.readFileToString(new File(fileName)).split("\n");
        for (String line : lines) {
            String[] fields = line.split("\t");
            String qid = fields[0];
            long docRank = Long.parseLong(fields[1]);
            String listType = fields[2];
            String itemList = fields[3];
            processor.process(new CandidateList(qid, docRank, listType, itemList));
        }
    }
    
    
    public static String [] splitItemList(String itemList) {
        return itemList.split("\\|");
    }
    
    public static String joinItemList(String [] items) {
        return Utility.join(items, "|");
    }
    
    public static String joinItemList(List<String> items) {
        return Utility.join(items, "|");
    }
}
