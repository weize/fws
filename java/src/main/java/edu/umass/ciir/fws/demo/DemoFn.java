/*
 *  BSD License (http://lemurproject.org/galago-license)
 */
package edu.umass.ciir.fws.demo;

import java.io.PrintStream;

import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.web.WebServer;


/**
 *
 * 
 */
public class DemoFn extends AppFunction {

  @Override
  public String getName() {
    return "demo";
  }

  @Override
  public String getHelpString() {
    return "";
  }

  @Override
  public void run(Parameters p, PrintStream output) throws Exception {
    Demo demo = new Demo(p);
    final DemoWebHandler searchHandler = new DemoWebHandler(demo);
    
    WebServer server = WebServer.start(p, searchHandler);

    output.println("Server: "+server.getURL());
  }
}
