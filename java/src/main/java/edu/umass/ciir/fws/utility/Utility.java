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
        return getReader(new File(filename));
    }

    public static BufferedReader getReader(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        if (file.getName().endsWith(".gz")) {
            return new BufferedReader(
                    new InputStreamReader(
                            new GZIPInputStream(stream)));
        } else {
            return new BufferedReader(
                    new InputStreamReader(stream));
        }
    }

    public static BufferedWriter getWriter(File file) throws IOException {
        if (file.getName().endsWith(".gz")) {
            return getGzipWriter(file);
        } else {
            return getNormalWriter(file);
        }
    }

    private static BufferedWriter getGzipWriter(File file) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file))));
    }

    private static BufferedWriter getNormalWriter(File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        return writer;
    }

    public static BufferedWriter getNormalWriter(String fileName) throws IOException {
        return getNormalWriter(new File(fileName));
    }

    public static BufferedWriter getWriter(String fileName) throws IOException {
        return getWriter(new File(fileName));
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

    public static void createDirectoryForFile(File file) {
        File dir = file.getParentFile();
        createDirectory(dir);
    }

    public static void createDirectoryForFile(String fileName) {
        createDirectoryForFile(new File(fileName));
    }

    public static String getCandidateListFileName(String clistDir, String qid, String suffix) {
        return getFileNameWithSuffix(clistDir, qid, suffix);
    }

    public static String getCandidateListCleanFileName(String clistDir, String qid) {
        return getFileNameWithSuffix(clistDir, qid, "clean.clist");
    }

    public static String getCandidateListRawFileName(String clistDir, String qid) {
        return getFileNameWithSuffix(clistDir, qid, "clist");
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

    public static String getCorpusCandidateListFileName(String clistCorpusDir, String docName, String suffix) {
        //clueweb09-en0009-30-02610
        //<clistCorpusDir>/en0009/clueweb09-en0009-30.clist.gz
        String[] subDirNames = docName.split("-");
        String name = String.format("%s-%s-%s.%s.gz", subDirNames[0], subDirNames[1], subDirNames[2], suffix);
        return getFileName(clistCorpusDir, subDirNames[1], name);
    }

    public static String getTermFeatureFileName(String featureDir, String qid) {
        return getFileNameWithSuffix(featureDir, qid, "t.feature");
    }

    public static String getGmTermDataFileName(String clusterDir, String qid) {
        return getFileNameWithSuffix(clusterDir, qid, qid, "t.data.gz");
    }

    public static String getGmTermPairDataFileName(String clusterDir, String qid) {
        return getFileNameWithSuffix(clusterDir, qid, qid, "p.data.gz");
    }

    public static String getGmTermPredictFileName(String clusterDir, String qid) {
        return getFileNameWithSuffix(clusterDir, qid, qid, "t.predict");
    }

    public static String getGmTermPairPredictFileName(String clusterDir, String qid) {
        return getFileNameWithSuffix(clusterDir, qid, qid, "p.predict");
    }

    public static String getQdFacetFeatureFileName(String qdFeatureDir, String qid) {
        return getFileNameWithSuffix(qdFeatureDir, qid, "f.feature");
    }

    public static String getQdClusterFileName(String clusterDir, String qid, double distanceMax, double websiteCountMin) {
        String name = String.format("%s.qd.%s.cluster", qid, parametersToFileNameString(distanceMax, websiteCountMin));
        return getFileName(clusterDir, qid, name);
    }

    public static String getQdFacetFileName(String facetDir, String qid, double distanceMax, double websiteCountMin, double itemRatio) {
        String name = String.format("%s.qd.%s.facet", qid, parametersToFileNameString(distanceMax, websiteCountMin, itemRatio));
        return getFileName(facetDir, qid, name);
    }

    public static String getPlsaClusterFileName(String clusterDir, String qid, long topNum) {
        String name = String.format("%s.plsa.%d.cluster", qid, topNum);
        return getFileName(clusterDir, qid, name);
    }

    public static String getPlsaFacetFileName(String facetDir, String qid, long plsaTopicNum, long plsaTermNum) {
        String name = String.format("%s.plsa.%s.facet", qid, parametersToFileNameString(plsaTopicNum, plsaTermNum));
        return getFileName(facetDir, qid, name);
    }

    public static String getLdaClusterFileName(String clusterDir, String qid, long topNum) {
        String name = String.format("%s.lda.%d.cluster", qid, topNum);
        return getFileName(clusterDir, qid, name);
    }

    public static String getLdaFacetFileName(String facetDir, String qid, long plsaTopicNum, long plsaTermNum) {
        String name = String.format("%s.lda.%s.facet", qid, parametersToFileNameString(plsaTopicNum, plsaTermNum));
        return getFileName(facetDir, qid, name);
    }

    public static String getGmiClusterFileName(String clusterDir, String qid, double termProbThreshold, double pairProbThreshould) {
        String name = String.format("%s.gmi.%s.cluster", qid, parametersToFileNameString(termProbThreshold, pairProbThreshould));
        return getFileName(clusterDir, qid, name);
    }

    public static String getGmiFacetFileName(String facetDir, String qid, double termProbThreshold, double pairProbThreshould) {
        String name = String.format("%s.gmi.%s.facet", qid, parametersToFileNameString(termProbThreshold, pairProbThreshould));
        return getFileName(facetDir, qid, name);
    }

    public static String getGmjClusterFileName(String clusterDir, String qid) {
        String name = String.format("%s.gmj.cluster", qid);
        return getFileName(clusterDir, qid, name);
    }

    public static String getGmjFacetFileName(String facetDir, String qid) {
        String name = String.format("%s.gmj.facet", qid);
        return getFileName(facetDir, qid, name);
    }

    public static String getFacetFileName(List<Object> run, String qid) {
        //run: dir model param...
        String facetDir = run.get(0).toString();
        String model = run.get(1).toString();
        String name;
        if (run.size() >= 3) {
            name = String.format("%s.%s.%s.facet", qid, model, parametersToFileNameString(run.subList(2, run.size())));
        } else {
            name = String.format("%s.%s.facet", qid, model);
        }
        return getFileName(facetDir, qid, name);
    }

    public static String getPoolFileName(String poolDir, String qid) {
        return getFileName(poolDir, qid + ".pool");
    }

    public static String getFileName(String... parts) {
        return TextProcessing.join(parts, File.separator);
    }

    public static String getFileNameWithSuffix(String... parts) {
        List<String> list = Arrays.asList(parts);
        int last = list.size() - 1;
        return TextProcessing.join(list.subList(0, last), File.separator) + "." + list.get(last);
    }

    public static String parametersToFileNameString(List<Object> parameters) {
        String str = parametersToString(parameters);
        return str.replace('.', '_');
    }

    public static String parametersToFileNameString(Object... parameters) {
        String str = parametersToString(parameters);
        return str.replace('.', '_');
    }

    public static String parametersToString(List<Object> parameters) {
        return TextProcessing.join(parameters, "-");
    }

    public static String parametersToString(Object... parameters) {
        return TextProcessing.join(parameters, "-");
    }

    public static String[] splitParameters(String parametersStr) {
        return parametersStr.split("-");
    }

    public static void infoWritten(File outfile) {
        System.err.println(String.format("Writte in %s", outfile.getAbsoluteFile()));
    }

    public static void infoProcessing(File infile) {
        System.err.println(String.format("Processing %s", infile.getAbsoluteFile()));
    }

    public static int compare(double one, double two) {
        return Double.compare(one, two);
    }

    public static void infoProcessingQuery(String qid) {
        System.err.println("processing query " + qid);
    }

    public static String getOracleExpandRunFileName(String runDir, String qid, String name) {
        return getFileNameWithSuffix(runDir, qid, name, "rank");
    }
}
