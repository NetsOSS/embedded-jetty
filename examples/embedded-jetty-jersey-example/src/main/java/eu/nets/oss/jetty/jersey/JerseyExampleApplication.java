package eu.nets.oss.jetty.jersey;


import org.glassfish.jersey.server.ResourceConfig;

public class JerseyExampleApplication extends ResourceConfig {
    public JerseyExampleApplication() {
        packages("eu.nets.oss.jetty.jersey");
    }
}
