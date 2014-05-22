/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.srank;

import edu.umass.ciir.fws.eval.TrecEvaluator;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.PrintStream;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class EvalPerQuery extends AppFunction {

    @Override
    public String getName() {
        return "eval-ranking";
    }

    @Override
    public String getHelpString() {
        return "fws eval-ranking --qrel=<qrelFile> --rank=<rankFile> --trecEval=<pathToTrecEval> --output=<outputFile> --trecOutput=<trecOutput>";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        String qrelFileName = p.getString("qrel");
        String rankFileName = p.getString("rank");
        String trecEval = p.getString("trecEval");
        File outfile = new File(p.getString("output"));
        File trecOutfile = new File(p.getString("trecOutput"));

        
        TrecEvaluator evaluator = new TrecEvaluator(trecEval);
        evaluator.evalAndOutput(qrelFileName, rankFileName, trecOutfile, outfile);

        Utility.infoWritten(trecOutfile);
        Utility.infoWritten(outfile);
    }
}
