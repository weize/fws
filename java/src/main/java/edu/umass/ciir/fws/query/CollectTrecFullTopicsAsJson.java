/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.query;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class CollectTrecFullTopicsAsJson extends AppFunction {

    @Override
    public String getName() {
        return "collect-fulltopic-as-json";
    }

    @Override
    public String getHelpString() {
        return "fws collect-fulltopic-as-json config.json";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {

        List<String> inputFileName = p.getAsList("queryXmlFile");
        List<File> xmlFiles = loadFiles(inputFileName);

        TrecFullTopicXmlParser parser = new TrecFullTopicXmlParser();
        File outfile = new File(p.getString("queryJsonFile"));
        BufferedWriter writer = Utility.getWriter(outfile);
        for (File xmlFile : xmlFiles) {
            List<Parameters> topics = parser.parse(xmlFile);
            for (Parameters topic : topics) {
                writer.write(topic.toPrettyString().replace('\n', ' '));
                writer.newLine();
            }
        }
        writer.close();
        Utility.infoWritten(outfile);
    }

    private List<File> loadFiles(List<String> fileNames) {
        ArrayList<File> files = new ArrayList<>();
        for (String fileName : fileNames) {
            File file = new File(fileName);
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                for (File subFile : file.listFiles()) {
                    files.add(subFile);
                }
            }
        }
        return files;
    }

}
