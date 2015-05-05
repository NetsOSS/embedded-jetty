package eu.nets.oss.jetty.sample;


import eu.nets.oss.jetty.ClasspathResourceHandler;
import eu.nets.oss.jetty.ContextPathConfig;
import eu.nets.oss.jetty.EmbeddedJettyBuilder;
import eu.nets.oss.jetty.EmbeddedSpringBuilder;
import eu.nets.oss.jetty.PropertiesFileConfig;
import eu.nets.oss.jetty.StaticConfig;
import eu.nets.oss.jetty.StdoutRedirect;
import org.springframework.web.context.WebApplicationContext;

import java.util.EventListener;

import static com.google.common.base.Throwables.propagate;
import static eu.nets.oss.jetty.EmbeddedSpringBuilder.createSpringContextLoader;
import static eu.nets.oss.jetty.EmbeddedSpringWsBuilder.createMessageDispatcherServlet;
import static eu.nets.oss.jetty.EmbeddedWicketBuilder.addWicketHandler;

public class StartJetty {
    public static void main(String... args) {

        boolean onServer = EmbeddedJettyBuilder.isStartedWithAppassembler();

        ContextPathConfig config;
        if (onServer) {
            config = new HerokuConfig(new PropertiesFileConfig());
        } else {
            config = new StaticConfig("/jettySample", 8080);
        }

        final EmbeddedJettyBuilder builder = new EmbeddedJettyBuilder(config, !onServer);

        if (onServer) {
            StdoutRedirect.tieSystemOutAndErrToLog();
            builder.addHttpAccessLogAtRoot();
        }

        WebApplicationContext ctx = EmbeddedSpringBuilder.createApplicationContext("VAS Core Application Context", ApplicationConfiguration.class);
        EventListener springContextLoader = createSpringContextLoader(ctx);
//        builder.addKeystore(10000);

        builder.createRootServletContextHandler("/ws")
                .addEventListener(springContextLoader)
                .addServlet(createMessageDispatcherServlet(WsServletConfiguration.class))
                .mountAtPath("/helloService.wsdl")
                .mountAtPath("/helloService");


        // Option 1: Separate context
        ClasspathResourceHandler rh1 = builder.createWebAppClasspathResourceHandler();
        builder.createRootServletContextHandler("/res").setResourceHandler(rh1);

        // Alt 2: Put resource handler on same path as wicket
        ClasspathResourceHandler rh2 = builder.createWebAppClasspathResourceHandler();
        EmbeddedJettyBuilder.ServletContextHandlerBuilder servletContextHandlerBuilder = addWicketHandler(builder, "/wicket", springContextLoader, SampleWicketApplication.class, true);
        // Temporary disabled while we're waiting for the outcome of a jetty bug on this
        servletContextHandlerBuilder.setResourceHandler(rh2);
        try {
            builder.startJetty();
            builder.verifyServerStartup();
        } catch (Exception e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            propagate(e);
        }

        if (!onServer) {
            String url = "/wicket/homePage";
            builder.startBrowserStopWithAnyKey(url);
        }
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


