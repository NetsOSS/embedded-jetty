package eu.nets.oss.jetty.sample;

import eu.nets.oss.jetty.*;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static eu.nets.oss.jetty.EmbeddedSpringBuilder.spring;
import static eu.nets.oss.jetty.EmbeddedWicketBuilder.wicket;

public class StartJetty {
    public static void main(String... args) {

        routeLoggingThroughSlf4j();

        EmbeddedJettyBuilder.create(config())
                .withAccessLog()
                .withWebAppClassPathResourceHandler("/res")
                .withClassPathResourceHandler("/assets", "/META-INF/resources/webjars")
                .withServletContextContributors("/wicket",
                        spring(ApplicationConfiguration.class),
                        wicket(SampleWicketApplication.class))
                .run();
    }

    private static void routeLoggingThroughSlf4j() {
        StdoutRedirect.tieSystemOutAndErrToLog();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static ContextPathConfig config() {
        ContextPathConfig config;
        if (EmbeddedJettyBuilder.isStartedWithAppassembler()) {
            config = new HerokuConfig(new PropertiesFileConfig());
        } else {
            config = new StaticConfig("/jettySample", 8080);
        }
        return config;
    }

    private static class HerokuConfig implements ContextPathConfig {

        private final PropertiesFileConfig delegate;

        private HerokuConfig(PropertiesFileConfig delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getContextPath() {
            return delegate.getContextPath();
        }

        @Override
        public int getPort() {

            String port = System.getenv("PORT");

            return port == null ? delegate.getPort() : Integer.parseInt(port);
        }
    }

}


