package eu.nets.oss.jetty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;


/**
 * Not-entirely Shameless ripoff from stackoverflow.com
 */
public class StdoutRedirect {

    public static void tieSystemOutAndErrToLog() {

        // Be sure log4j has been initialized here, which is always the case for embedded-jetty.
        //  this shameless ripoff causes an infinite loop ehn log4j has not been properly initialized...
        //log4j -> logs to standard-err when it finds no appenders, standard err has been redirected using slf 4j
        //and here we go...
        System.setOut(createLoggingProxy(System.out, "stdout", true));
        System.setErr(createLoggingProxy(System.err, "stderr", false));
    }


    private static PrintStream createLoggingProxy(final PrintStream realPrintStream, final String stream, final boolean stdout) {
        return new PrintStream(realPrintStream) {

            private Logger logger = LoggerFactory.getLogger(stdout ? "stdout" : "stderr");


            public void print(final String string) {
                List<String> lines = Arrays.asList(string.split("[\r\n]+"));
                for (String line : lines) {
                    if (stdout)
                        logger.info(line);
                    else
                        logger.warn(line);
                }
            }

            @Override
            public void print(Object obj) {
                print(String.valueOf(obj));
            }

            @Override
            public void println(String s) {
                print(s);
            }

            @Override
            public void println(Object obj) {
                print(String.valueOf(obj));
            }
        };
    }
}
