package eu.nets.utils.jetty.embedded;

import com.google.common.base.Splitter;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Todo: Think about this design. It is inherently not a very good design, since it
 * does two things: Both apply the config and expose it through ContextPathConfig.
 */
public class PropertiesFileConfig implements ContextPathConfig {


    public PropertiesFileConfig() {
        applyEnvironment();
    }

    private void applyEnvironment() {
        InputStream is;
        File envFile = new File(getBasedir(), "environment.properties");
        // zOMG what a dirty block of code.
        if (envFile.exists()){
            try {
                is = new FileInputStream(envFile);
                Logger.info(this.getClass(), "Loading properties from " + envFile.getAbsolutePath());
            } catch (FileNotFoundException e) {
                Logger.warn(this.getClass(), "Could not load 'environment.properties', using system defaults");
                return;
            }
        } else {
            is = this.getClass().getResourceAsStream("/environment.properties");
            if (is == null) {
                Logger.warn(this.getClass(), "Could not load 'environment.properties' from file system or classpath, using system defaults");
                return;
            }
            Logger.info(this.getClass(), "Loading properties from classpath resource environment.properties");
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("ISO-8859-1")));
            System.getProperties().load(reader);
        } catch (IOException e) {
            Logger.warn(this.getClass(), "Could not load 'environment.properties', using system defaults");
        }
    }

    private String getBasedir() {
        String path = System.getProperty("basedir");
        // cygwin hack
        if (path != null && path.startsWith("/cygdrive")) {
            char drive = path.charAt(10);
            path = drive + ":" + path.substring(11);
        }
        return path;
    }

    public String getContextPath() {
        return System.getProperty("contextPath", "/jetty");
    }

    public int getPort() {
        return Integer.parseInt(System.getProperty("port", "9090"));
    }

    public Iterable<String> getIpWhiteList() {
        return Splitter.on(",").split(System.getProperty("ipWhiteList", ""));
    }
}
