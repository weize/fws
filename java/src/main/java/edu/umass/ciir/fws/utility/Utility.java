package edu.umass.ciir.fws.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * Created by wkong on 4/1/14.
 */
public class Utility extends org.lemurproject.galago.tupleflow.Utility {

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

    public static boolean createDirectory(String directoryName) {
        File dir = new File(directoryName);

        if (!dir.exists()) {
            return dir.mkdir();
        } else {
            //System.err.println(String.format("Directory %s exists!", directoryName));
        }
        return true;
    }

    public static String getCandidateListFileName(String clistDir, String qid, String suffix) {
        return String.format("%s%s%s.%s", clistDir, File.separator, qid, suffix);
    }

    public static String getParsedDocFileName(String parseDir, String qid, String docName) {
        return String.format("%s%s%s%s%s.parse",
                parseDir, File.separator, qid, File.separator, docName);
    }

    public static String getParsedDocDirName(String parseDir, String qid) {
        return String.format("%s%s%s", parseDir, File.separator, qid);
    }

    public static String getDocFileName(String docDir, String qid, String docName) {
        return String.format("%s%s%s%s%s.html",
                docDir, File.separator, qid, File.separator, docName);
    }

    public static String getDocDirName(String docDir, String qid) {
        return String.format("%s%s%s", docDir, File.separator, qid);
    }

}
