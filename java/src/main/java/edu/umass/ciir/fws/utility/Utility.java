package edu.umass.ciir.fws.utility;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
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
        return getFileNameWithSuffix(clistDir, qid, suffix);
    }

    public static String getParsedDocFileName(String parseDir, String qid, String docName) {
        return getFileNameWithSuffix(parseDir, qid, docName, "parse");
    }

    public static String getDocHtmlFileName(String docDir, String qid, String docName) {
        return getFileNameWithSuffix(docDir, qid, docName, "html");
    }

    public static String getDocFileDir(String docDir, String qid) {
        return getFileName(docDir, qid);
    }
    
    public static String getDocDataFileName(String docDir, String qid, String docName) {
        return getFileNameWithSuffix(docDir, qid, docName, "dat");
    }

    public static String getParsedCorpusDocFileName(String corpusDocDir, String docName) {
        String[] subDirNames = docName.split("-");
        return getFileNameWithSuffix(corpusDocDir, subDirNames[1], subDirNames[2], docName, "parse.gz");
    }

    public static String getTermFeatureFileName(String featureDir, String qid) {
        return getFileNameWithSuffix(featureDir, qid, "t.feature");
    }

    public static String getQdFacetFeatureFileName(String qdFeatureDir, String qid) {
        return getFileNameWithSuffix(qdFeatureDir, qid, "f.feature");
    }

    public static String getQdClusterFileName(String clusterDir, String qid, double distanceMax, double websiteCountMin) {
        String name = String.format("%s.%s.cluster", qid, parametersToFileNameString(distanceMax, websiteCountMin));
        return getFileName(clusterDir, qid, name);
    }

    public static String getQdFacetFileName(String facetDir, String qid, double distanceMax, double websiteCountMin, double itemRatio) {
        String name = String.format("%s.%s.facet", qid, parametersToFileNameString(distanceMax, websiteCountMin, itemRatio));
        return getFileName(facetDir, qid, name);
    }
    
    public static String getFileName(String... parts) {
        return TextProcessing.join(parts, File.separator);
    }

    public static String getFileNameWithSuffix(String... parts) {
        List<String> list = Arrays.asList(parts);
        int last = list.size() - 1;
        return TextProcessing.join(list.subList(0, last), File.separator) + "." + list.get(last);
    }

    public static String parametersToFileNameString(Object... parameters) {
        String str = parametersToString(parameters);
        return str.replace('.', '_');
    }

    public static String parametersToString(Object... parameters) {
        return TextProcessing.join(parameters, ":");
    }

    public static String[] splitParameters(String parametersStr) {
        return parametersStr.split(":");
    }

    

}
