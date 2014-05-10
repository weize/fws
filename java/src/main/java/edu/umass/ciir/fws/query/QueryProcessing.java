/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.query;

import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.TextProcessing;

/**
 *
 * @author wkong
 */
public class QueryProcessing {
    
    public static TfQuery toSDMQuery(TfQuery query) {
        String text = "#sdm (" + TextProcessing.clean(query.text) + " )";
        TfQuery newQuery = new TfQuery(query.id, text);
        return newQuery;
    }
    
}
