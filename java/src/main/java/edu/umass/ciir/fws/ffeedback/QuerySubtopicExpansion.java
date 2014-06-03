/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.types.TfQueryExpansion;
import edu.umass.ciir.fws.types.TfQueryExpansionSubtopic;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author wkong
 */
public class QuerySubtopicExpansion extends QueryExpansion {

    String sid;

    public QuerySubtopicExpansion(String qid, String sid, String oriQuery, String model, String expansion, long expId) {
        super(qid, oriQuery, model, expansion, expId);
        this.sid = sid;
        this.id = toId(qid, sid, model, expId);
    }

    public QuerySubtopicExpansion(QueryExpansion qe, String sid) {
        super(qe.qid, qe.oriQuery, qe.model, qe.expansion, qe.expId);
        this.sid = sid;
        this.id = toId(qid, sid, model, expId);
    }
    
    public QuerySubtopicExpansion(TfQueryExpansion qe, String sid) {
        super(qe.qid, qe.oriQuery, qe.model, qe.expansion, qe.expId);
        this.sid = sid;
        this.id = toId(qid, sid, model, expId);
    }

    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%d\t%s\t%s\n", qid, sid, model, expId, expansion, oriQuery);
    }

    public static QuerySubtopicExpansion parse(String text) {
        String[] elems = text.split("\t");
        String qid = elems[0];
        String sid = elems[1];
        String model = elems[2];
        Long expId = Long.parseLong(elems[3]);
        String expansion = elems[4]; // expansion may be empty
        String oriQuery = elems[5]; // original query

        return new QuerySubtopicExpansion(qid, sid, oriQuery, model, expansion, expId);
    }

    public TfQueryExpansionSubtopic toTfQueryExpansionSubtopic() {
        return new TfQueryExpansionSubtopic(qid, model, expId, sid);
    }

    public static String toId(TfQueryExpansionSubtopic qes) {
        return toId(qes.qid, qes.sid, qes.model, qes.expId);
    }
    
    public static String toId(String qid, String sid, String model, long expId) {
        return String.format("%s-%s-%s-%d", qid, sid, model, expId);
    }

    public static HashMap<String, QuerySubtopicExpansion> loadQueryExpansionAsMap(File file, String model) throws IOException {
        List<QuerySubtopicExpansion> qseList = load(file, model);
        HashMap<String, QuerySubtopicExpansion> map = new HashMap<>();
        for (QuerySubtopicExpansion qse : qseList) {
            map.put(qse.id, qse);
        }
        return map;
    }

    public static List<QuerySubtopicExpansion> load(File file, String model) throws IOException {
        ArrayList<QuerySubtopicExpansion> list = new ArrayList<>();
        BufferedReader reader = Utility.getReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().startsWith("#")) {
                QuerySubtopicExpansion qse = QuerySubtopicExpansion.parse(line);
                if (qse.model.equals(model)) {
                    list.add(qse);
                }
            }
        }
        reader.close();
        return list;
    }
}
