package eu.nets.oss.jetty;

import java.io.*;
import java.nio.charset.Charset;


/**
 * Loading Secure environment properties AFTER the regular environment.properties.
 * It is important to read AFTER, so that the secure props cannot be overridden by regular env props.
 *
 * Secure Environment Properties shall only be readable from file - never from class path.
 */
public class SecurePropertiesFileConfig extends PropertiesFileConfig {


    private final String fileName = "secure/secure-environment.properties";

    public SecurePropertiesFileConfig() {
        super();
        applySecureEnvironment();
    }



    private void applySecureEnvironment() {
        InputStream is;
        File envFile = new File(getBasedir(), fileName);

        if (envFile.exists()){
            try {
                is = new FileInputStream(envFile);
                Logger.info(this.getClass(), "Loading secure properties from " + envFile.getAbsolutePath());

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("ISO-8859-1")));
                System.getProperties().load(reader);

            } catch (IOException e) {
                Logger.warn(this.getClass(), "Could not load '" + fileName  + "', using system defaults");
            }

        } else {
            Logger.warn(this.getClass(), "No file '" + fileName  + "' found.");
        }

    }


}

