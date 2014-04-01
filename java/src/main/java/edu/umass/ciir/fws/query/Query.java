package edu.umass.ciir.fws.query;

/**
 * Created by wkong on 4/1/14.
 */
public class Query {
    String id;
    String text;

    public Query(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public Query(String line) {
        String [] fields = line.split("\t");
        this.id = fields[0];
        this.text = fields[1];
    }

}
