/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.query;

import edu.umass.ciir.fws.types.Query;
import edu.umass.ciir.fws.utility.TextProcessing;

/**
 *
 * @author wkong
 */
public class QueryProcessing {
    
    public static Query toSDMQuery(Query query) {
        String text = "#sdm" + TextProcessing.clean(query.text) + " )";
        Query newQuery = new Query(query.id, text);
        return newQuery;
    }
    
}
