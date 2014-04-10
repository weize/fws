package edu.umass.ciir.fws.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    public static BufferedWriter getNormalWriter(String filename) throws IOException {
        File file = new File(filename);
        BufferedWriter writer = null;
        FileWriter f = new FileWriter(filename);
        writer = new BufferedWriter(f);
        return writer;
    }

    public static BufferedWriter getWriter(String filename) throws IOException {
        if (filename.endsWith(".gz")) {
            return getGzipWriter(filename);
        } else {
            return getNormalWriter(filename);
        }
    }

    public static BufferedWriter getGzipWriter(String filename) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filename))));
    }

    public static boolean createDirectory(String directoryName) {
        File dir = new File(directoryName);
        createDirectory(dir);
        return true;
    }

    public static boolean createDirectory(File dir) {
        if (!dir.exists()) {
            return dir.mkdirs();
        } else {
            //System.err.println(String.format("Directory %s exists!", directoryName));
        }
        return true;
    }

    public static void createDirectoryForFile(String fileName) {
        File file = new File(fileName);
        File dir = file.getParentFile();
        createDirectory(dir);
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

    public static String getParsedCorpusDocFileName(String corpusDocDir, String docName) {
        String[] subDirNames = docName.split("-");
        return String.format("%s%s%s%s%s%s%s.parse.gz", corpusDocDir, File.separator, subDirNames[1], File.separator, subDirNames[2],
                File.separator, docName);
    }

}
