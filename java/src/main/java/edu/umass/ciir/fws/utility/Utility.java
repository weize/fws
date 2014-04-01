package edu.umass.ciir.fws.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * Created by wkong on 4/1/14.
 */
public class Utility {

    public static BufferedReader getReader(String filename) throws IOException {
        BufferedReader reader = null;
        FileInputStream stream = new FileInputStream(filename);
        if (filename.endsWith(".gz")) {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new GZIPInputStream(stream)));
        } else {
            reader = new BufferedReader(
                    new InputStreamReader(stream));
        }

        return reader;
    }
    
     public static BufferedWriter getWriter(String filename) throws IOException {
        BufferedWriter writer = null;
        FileWriter f = new FileWriter(filename);
        writer = new BufferedWriter(f);
        return writer;
    }
}
