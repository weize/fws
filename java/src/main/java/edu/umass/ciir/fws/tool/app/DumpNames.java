/*
 *  BSD License (http://lemurproject.org/galago-license)
 */
package edu.umass.ciir.fws.tool.app;

import java.io.PrintStream;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.retrieval.iterator.DataIterator;
import org.lemurproject.galago.core.retrieval.iterator.LengthsIterator;
import org.lemurproject.galago.core.retrieval.processing.ScoringContext;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author sjh
 */
public class DumpNames extends AppFunction {

    @Override
    public String getName() {
        return "dump-name";
    }

    @Override
    public String getHelpString() {
        return "fws dump-name --index=[indexPath]\n"
                + "Dump document names from index\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        if (!p.containsKey("index")) {
            output.println(this.getHelpString());
        }

        DiskIndex index = new DiskIndex(p.getString("index"));

        DataIterator<String> namesItr = index.getNamesIterator();

        ScoringContext sc = new ScoringContext();

        while (!namesItr.isDone()) {
            long docId = namesItr.currentCandidate();
            sc.document = docId;
            String docName = namesItr.data(sc);
            output.println(docName);
            namesItr.movePast(docId);
        }
    }
}
