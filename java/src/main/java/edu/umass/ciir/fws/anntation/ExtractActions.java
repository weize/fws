package edu.umass.ciir.fws.anntation;


import edu.umass.ciir.fws.anntation.Action;
import edu.umass.ciir.fws.anntation.FeedbackAnnotation;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author wkong
 */
public class ExtractActions extends AppFunction {

    @Override
    public String getName() {
        return "extract-actions";
    }

    @Override
    public String getHelpString() {
        return "fws extract-actions config.json";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {

        File jsonFile = new File(p.getString("actionJson"));
        List<Action> actions = Action.load(jsonFile);

        File file = new File(p.getString("actionText"));
        extractActionText(actions, file);
        Utility.infoWritten(file);

    }

    private void extractActionText(List<Action> actions, File outfile) throws IOException {
        BufferedWriter writer = Utility.getWriter(outfile);
        for (Action action : actions) {
            writer.write(action.toString());
            writer.newLine();
        }
        writer.close();

    }
}
