/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.query;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.util.StreamReaderDelegate;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class TrecFullTopicXmlParser {

    enum Status {

        inQuery, inDescription, inSubtopic, other
    }

    StreamReaderDelegate reader;
    XMLInputFactory factory;
    Parameters topic;
    ArrayList<Parameters> subtopics;
    Parameters subtopic;
    Status status;

    public TrecFullTopicXmlParser() {
        // XML processing
        factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    public List<Parameters> parse(File file) throws IOException {
        reader = null;
        InputStream is = null;
        status = Status.other;
        ArrayList<Parameters> topics = new ArrayList<>();
        try {
            is = new FileInputStream(file);
            reader = new StreamReaderDelegate(factory.createXMLStreamReader(is, "UTF-8"));
            while (reader.hasNext()) {
                int type = reader.next();

                if (type == XMLStreamConstants.START_ELEMENT) {
                    String name = reader.getLocalName();
                    switch (name) {
                        case "topic":
                            processTopic();
                            break;
                        case "query":
                            status = Status.inQuery;
                            break;
                        case "description":
                            status = Status.inDescription;
                            break;
                        case "subtopic":
                            status = Status.inSubtopic;
                            processSubtopic();
                            break;
                    }
                } else if (type == XMLStreamConstants.END_ELEMENT) {
                    String name = reader.getLocalName();
                    switch (name) {
                        case "topic":
                            topics.add(topic);
                            break;
                        case "query":
                        case "description":
                            status = Status.other;
                            break;
                        case "subtopic":
                            status = Status.other;
                            break;
                    }
                } else if (type == XMLStreamConstants.CHARACTERS) {
                    String content = reader.getText().replaceAll("\\s+", " ").trim(); // remove new lines
                    switch (status) {
                        case inQuery:
                            topic.set("query", content);
                            break;
                        case inDescription:
                            topic.set("description", content);
                            break;
                        case inSubtopic:
                            subtopic.set("description", content);
                            break;

                    }
                }
            }
        } catch (Exception e) {
            // Something bad happened - close the file and complain
            System.err.printf(
                    "Unable to finish processing file %s. Closing and continuing.\n",
                    file.getAbsoluteFile());
            e.printStackTrace();
        } finally {
            if (is != null) {
                is.close();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (XMLStreamException xse) {
                System.err.printf("Caught stream exception: %s\n", xse.getMessage());
                xse.printStackTrace(System.err);
                System.err.printf("RECONVERT:\t%s\n", file.getAbsoluteFile());
                // throw new IOException(xse);
            }
        }

        return topics;
    }

    private void processTopic() {
        String number = reader.getAttributeValue(null, "number");
        String type = reader.getAttributeValue(null, "type");

        topic = new Parameters();
        subtopics = new ArrayList<>();
        topic.set("subtopics", subtopics);
        topic.set("number", number);
        topic.set("type", type);
    }

    private void processSubtopic() {
        String number = reader.getAttributeValue(null, "number");
        String type = reader.getAttributeValue(null, "type");

        subtopic = new Parameters();
        subtopics.add(subtopic);
        subtopic.set("number", number);
        subtopic.set("type", type);
    }

}
