package eu.nets.oss.jetty;

import java.io.*;
import java.util.Properties;

import static java.lang.Integer.parseInt;

/**
 * Shameless rip from shared-server
 *
 * Users of embedded-jetty really don't need much of the java code in
 * shared-server. And it just adds a bunch of weird dependencies.
 *
 * But this stuff you /will/ need :)
 */
public class ServerUtil {

    public static String readConfigurationParameter(String propertyName) {
        return readConfigurationParameter(propertyName, null);
    }


    public static int readConfigurationParameter(String propertyName, int defaultValue) {
        return parseInt(readSecureConfigurationParameter(propertyName,
                Integer.toString(defaultValue)));
    }

    public static String readConfigurationParameter(String propertyName, String defaultValue) {
        return readSecureConfigurationParameter(propertyName, defaultValue);
    }

    private static String readSecureConfigurationParameter(String propertyName, String defaultValue) {
        String value;
        if ((value = System.getProperty(propertyName)) != null) {
            return value;
        } else if ((value = getEnvironmentFile(getSecureConfigFileName()).getProperty(propertyName)) != null) {
            return value;
        } else if ((value = getEnvironmentFile(getConfigFileName()).getProperty(propertyName)) != null) {
            return value;
        } else if (defaultValue != null) {
            return defaultValue;
        } else {
            throw new IllegalArgumentException(
                    "Configuration error: Missing property [" + propertyName + "]. " +
                            "Add " + propertyName + "=<value> to " +
                            getConfigFileName().getPath() +
                            " or supply as an execution parameter " +
                            "-D" + propertyName + "=<value>");
        }
    }

    /**
     * Leser properties fra classpath og vil overlaste i reverse order, slik at
     * classpath reverse order styrer hvilke props som gjelder.
     *
     * @param configFile the file to read properties from
     * @return Properties med properties i overridden form
     * @throws {@link java.io.IOException}, {@link RuntimeException}
     */
    private static Properties getEnvironmentFile(File configFile) {
        Properties mergedProperties = new Properties();

        InputStream inputStream;
        try {
            inputStream = new FileInputStream(configFile);
        } catch (FileNotFoundException e) {
            // File does not exist. That's all right.
            return mergedProperties;
        }
        try {
            mergedProperties.load(inputStream);
            inputStream.close();
            return mergedProperties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File getConfigFileName() {
        try {
            return new File(System.getProperty("user.dir"),
                    "environment.properties").getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

    private static File getSecureConfigFileName() {
        try {
            return new File(System.getProperty("user.dir") + "/secure/",
                    "secure-environment.properties").getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

}
