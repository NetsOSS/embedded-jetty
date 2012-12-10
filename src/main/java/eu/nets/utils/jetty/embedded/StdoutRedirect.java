package eu.nets.utils.jetty.embedded;

import org.slf4j.LoggerFactory;

import java.io.PrintStream;

import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.Splitter.on;


/**
 * Shameless ripoff from stackoverflow.com
 */
public class StdoutRedirect {

    public static void tieSystemOutAndErrToLog() {
        //TODO: this shameless ripoff causes an infinite loop ehn log4j has not been properly initialized...
        //log4j -> logs to standard-err when it finds no appenders, standard err has been redirected using slf 4j-----
        //and here we go...
        // Use threadlocal to signal that we're already logging ?

        //System.setOut(createLoggingProxy(System.out));
        //System.setErr(createLoggingProxy(System.err));
    }

    private static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                Iterable<String> lines = on(anyOf("\r\n")).split(string);
                for (String line : lines) {
                    LoggerFactory.getLogger("stdout").info(line);
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
