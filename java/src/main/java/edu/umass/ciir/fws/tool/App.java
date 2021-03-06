package edu.umass.ciir.fws.tool;

import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.GalagoConf;
import org.lemurproject.galago.tupleflow.Parameters;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by wkong on 4/1/14.
 */
public class App {
    /**
     * function selection and processing
     */
    public final static Logger log;
    public final static HashMap<String, AppFunction> appFunctions;

    // init function -- allows internal use of app function library
    static {
        log = LoggerFactory.getLogger("fws-App");
        appFunctions = new HashMap();

        // list of classpaths to scan
        List<String> cps = new ArrayList();
        cps.add("edu.umass.ciir.fws");

        Parameters p = GalagoConf.getAllOptions();
        if (p.isString("appclasspath") || p.isList("appclasspath", String.class)) {
            cps.addAll((List<String>) p.getAsList("appclasspath"));
        }

        for (String cp : cps) {
            Reflections reflections = new Reflections(cp);
            Set<Class<? extends AppFunction>> apps = reflections.getSubTypesOf(AppFunction.class);

            for (Class c : apps) {
                try {
                    Constructor cons = c.getConstructor();
                    AppFunction fn = (AppFunction) cons.newInstance();
                    String name = fn.getName();

                    // if we have a duplicated function - use the first one.
                    if (appFunctions.containsKey(fn.getName())) {
                        log.info("Found duplicated function name: " + c.getName() + ". Arbitrarily using: " + appFunctions.get(name).getClass().getName());
                    } else {
                        appFunctions.put(fn.getName(), fn);
                    }
                } catch (Exception e) {
                    log.info("Failed to find constructor for app: {0}", c.getName());
                }
            }
        }
    }

    /*
     * Eval function
     */
    public static void main(String[] args) throws Exception {
        App.run(args);
    }

    public static void run(String[] args) throws Exception {
        run(args, System.out);
    }

    public static void run(String[] args, PrintStream out) throws Exception {
        String fn = "help";

        if (args.length > 0 && appFunctions.containsKey(args[0])) {
            fn = args[0];
        }
        appFunctions.get(fn).run(args, out);
    }

    public static void run(String fn, Parameters p, PrintStream out) throws Exception {
        if (appFunctions.containsKey(fn)) {
            appFunctions.get(fn).run(p, out);
        } else {
            log.warn("Could not find app: " + fn);
        }
    }

}
