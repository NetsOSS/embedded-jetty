package eu.nets.utils.jetty.embedded;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.JavaUtilLog;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;

import javax.servlet.Servlet;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;

import static java.awt.Desktop.getDesktop;
import static java.lang.String.format;
import static java.net.InetAddress.getLocalHost;


/**
 * A jetty builder that can handle "all" jetty building needs with a fluent api ;)
 * <p/>
 * An embedded jetty builder accepts a root context path. All handlers added to the builder
 * are appended to this root path.
 *
 * @author Kristian Rosenvold
 */
public class EmbeddedJettyBuilder {
    private final Server server;
    private final String contextPath;
    private final int port;
    private final boolean devMode;
    private IPAccessHandler secureWrap;
    private final LinkedList<Exception> startupExceptions = new LinkedList<Exception>();
    private long initTime;
    List<HandlerBuilder> handlers = new ArrayList<HandlerBuilder>();

    /**
     * Create a new builder.
     *
     * @param context The context defining the root path and port of the application
     * @param devMode true to run in development mode, which normally caches less content.
     */
    public EmbeddedJettyBuilder(ContextPathConfig context, boolean devMode) {
        this.contextPath = context.getContextPath();
        this.port = context.getPort();
        this.devMode = devMode;
        server = createServer(port, devMode);
    }

    public static class HandlerBuilder<T extends Handler> {
        private final T handler;

        public HandlerBuilder(T handler) {
            this.handler = handler;
        }

        public T getHandler() {
            return handler;
        }

        public void setResourceHandler(ResourceHandler resourceHandler) {
            ((ContextHandler) handler).setHandler(resourceHandler);
        }
    }

    public class ServletContextHandlerBuilder extends HandlerBuilder<ServletContextHandler> {
        private final ServletContextHandler handler;

        public ServletContextHandlerBuilder(ServletContextHandler handler) {
            super(handler);
            this.handler = handler;
            setHttpCookieOnly(true);
        }

        public ServletHolderBuilder addServlet(Servlet servlet, String pathSpec) {
            return new ServletHolderBuilder(this, servlet, pathSpec);
        }

        public ServletContextHandlerBuilder setHttpCookieOnly(boolean httpCookieOnly) {
            handler.getSessionHandler().getSessionManager().getSessionCookieConfig().setHttpOnly(httpCookieOnly);
            return this;
        }

        /**
         * Adds an Event Listener to this servlet context, typically some implementation of ServletContextListener
         *
         * @param eventListener The event listener to add
         * @return this builder
         */
        public ServletContextHandlerBuilder addEventListener(EventListener eventListener) {
            handler.addEventListener(eventListener);
            return this;
        }

        public ServletContextHandlerBuilder setSecurityHandler(SecurityHandler securityHandler) {
            handler.setSecurityHandler(securityHandler);
            return this;
        }

        public ServletContextHandlerBuilder setClassLoader(ClassLoader classLoader) {
            handler.setClassLoader(classLoader);
            return this;
        }
    }

    public class ServletHolderBuilder {
        private final ServletHolder sh;
        private final EmbeddedJettyBuilder.ServletContextHandlerBuilder servletContext;

        public ServletHolderBuilder(ServletContextHandlerBuilder servletContext, Servlet servlet, String pathSpec) {
            sh = new ServletHolder(servlet);
            this.servletContext = servletContext;
            mountAtPath(pathSpec);
        }

        public ServletHolderBuilder mountAtPath(String pathSpec) {
            this.servletContext.handler.addServlet(sh, pathSpec);
            return this;

        }

        public ServletHolderBuilder setInitParameter(String param, String value) {
            sh.setInitParameter(param, value);
            return this;

        }
    }

    private void setPath(ContextHandler handler, String usePath) {
        getLogger().info(">>>> Context handler added at " + usePath + " <<<<");
        handler.setContextPath(usePath);

    }

    /**
     * Creates a HandlerBuilder that is mounted on top of the root path of this builder
     *
     * @param subPath The sub-path that will be appended, starting with a slash, or just an empty string for no subpath
     * @return A handler builder. This can not be used for servlets, see #createRootServletContextHandler
     */
    public HandlerBuilder createRootContextHandler(String subPath) {
        ContextHandler contextHandler = new ContextHandler();
        HandlerBuilder<ContextHandler> e = new HandlerBuilder<ContextHandler>(contextHandler);
        String usePath = contextPath + subPath;
        setPath(contextHandler, usePath);
        handlers.add(e);
        return e;
    }

    /**
     * Creates a ServletContextHandlerBuilder that is mounted on top of the root path of this builder
     *
     * @param subPath The sub-path that will be appended, starting with a slash, or just an empty string for no subpath
     * @return A handler builder
     */
    public ServletContextHandlerBuilder createRootServletContextHandler(String subPath) {
        ServletContextHandler handler = getServletContextHandler();
        ServletContextHandlerBuilder e = new ServletContextHandlerBuilder(handler);
        String usePath = contextPath + subPath;
        setPath(e.getHandler(), usePath);
        handlers.add(e);
        return e;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void addWhiteList(Iterable<String> allowedIps) {
        secureWrap = new IPAccessHandler();
        for (String allowedIp : allowedIps) {
            secureWrap.addWhite(allowedIp);
        }
    }

    private ServletContextHandler getServletContextHandler() {
        return new ServletContextHandler(null, contextPath, ServletContextHandler.SESSIONS) {
            @Override
            protected void startContext() throws Exception {
                try {
                    super.startContext();
                } catch (Exception e) {
                    startupExceptions.add(e);
                    throw e;
                }
            }
        };
    }

    private Server createServer(int port, boolean devMode) {
        Server server = new Server();
        Connector connector = new SelectChannelConnector();
        connector.setPort(port);

        if (devMode) {
            connector.setMaxIdleTime(1000000);
        } else {
            connector.setMaxIdleTime(30000);
            server.setStopAtShutdown(true);
            server.setGracefulShutdown(2000);
        }
        connector.setMaxIdleTime(10000000);

        //connector.setSoLingerTime(-1);
        server.addConnector(connector);
        return server;
    }

    /**
     * check if a keystore for a SSL certificate is available, and
     * if so, start a SSL connector on port 8443. By default, the
     * quickstart comes with a Apache Wicket Quickstart Certificate
     * that expires about half way september 2021. Do not use this
     * certificate anywhere important as the passwords are available
     * in the source.
     *
     * @param timeout a timeout ;)
     */
    public void addKeystore(int timeout) {
        Resource keystore = Resource.newClassPathResource("/keystore");
        if (keystore != null && keystore.exists()) {
            //connector.setConfidentialPort(8443);
            org.eclipse.jetty.util.ssl.SslContextFactory factory = new org.eclipse.jetty.util.ssl.SslContextFactory();
            factory.setKeyStoreResource(keystore);
            factory.setKeyStorePassword("wicket");
            factory.setTrustStore("/keyStore");
            factory.setKeyManagerPassword("wicket");
            SslSocketConnector sslConnector = new SslSocketConnector(factory);
            sslConnector.setMaxIdleTime(timeout);
            sslConnector.setPort(8443);
            sslConnector.setAcceptors(4);
            server.addConnector(sslConnector);

            System.out.println("SSL access to the quickstart has been enabled on port 8443");
            System.out.println("You can access the application using SSL on https://localhost:8443");
            System.out.println();
        }
    }

    Server buildJetty() {
        HandlerList hl = new HandlerList();
        for (HandlerBuilder handler : handlers) {
            hl.addHandler(handler.getHandler());
        }

        final Handler handlerToUse;
        if (secureWrap != null) {
            secureWrap.setHandler(hl);
            handlerToUse = secureWrap;
        } else {
            handlerToUse = hl;
        }

        server.setHandler(handlerToUse);
        return server;
    }

    public void startJetty() {
        this.initTime = System.currentTimeMillis();
        buildJetty();
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stopJetty() {
        try {
            server.stop();
            server.join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @SuppressWarnings("UnusedDeclaration")
    public EmbeddedJettyBuilder addHttpAccessLogAtRoot() {
        handlers.add(new HandlerBuilder<Handler>(new RequestLogHandler()));
        return this;
    }

    public LinkedList<Exception> getStartupExceptions() {
        return startupExceptions;
    }

    @SuppressWarnings("UnusedDeclaration")
    private static void checkIfServerIsRunning(final int port) {
        try {
            final String hostAddress = failsafeGetHostname();
            new Socket(hostAddress, port).close();
            throw new IllegalArgumentException(String.format("A runnning server was found on '%s', port '%d'",
                    hostAddress, port));
        } catch (IOException e) {
            //
        }
    }

    private static String failsafeGetHostname() throws UnknownHostException {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            getLogger().info("Unable to retrieve hostname", e);
            return "localhost";
        }
    }


    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void verifyServerStartup() {
        org.eclipse.jetty.util.log.Logger logger = new JavaUtilLog(EmbeddedJettyBuilder.class.getName());


        if (!startupExceptions.isEmpty()) {
            Exception exception = startupExceptions.peekFirst();
            logger.warn(format("Errors during startup. The first is %s: %s", exception.getClass().getName(), exception.getMessage()));
            System.exit(-1);
        }

        long startupTime = (System.currentTimeMillis() - initTime);
        String ip = "0.0.0.0";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignore) {/* care face */
        }
        logger.info(format("Server started on http://%s:%d%s in %dms", ip, port, contextPath, startupTime));
    }

    protected static JavaUtilLog getLogger() {
        return new JavaUtilLog(EmbeddedJettyBuilder.class.getName());
    }


    public static ConstraintMapping getConstraintMapping(Constraint constraint, String pathSpec) {
        ConstraintMapping cm2 = new ConstraintMapping();
        cm2.setConstraint(constraint);
        cm2.setPathSpec(pathSpec);
        return cm2;
    }


    @SuppressWarnings({"ResultOfMethodCallIgnored", "UnusedDeclaration"})
    public void startBrowserStopWithAnyKey(String url) {
        System.out.println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP");
        try {
            getDesktop().browse(getUri(url));
            System.in.read();
            System.out.println(">>> STOPPING EMBEDDED JETTY SERVER");
            stopJetty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URI getUri(String url) {
        try {
            return URI.create("http://" + getLocalHost().getHostAddress() + ":" + port + contextPath + url);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a standard resource handler that exposes all resources under /webapp as web content
     *
     * @param subContextPath The path to map the resources at
     */
    public void createStandardClasspathResourceHandler(String subContextPath) {
        boolean useCaches = !devMode;
        createRootContextHandler(subContextPath).setResourceHandler(new ClasspathResourceHandler("/webapp", useCaches));
    }

    /**
     * @return true if the current process has been started with appassambler
     */
    public static boolean isStartedWithAppassembler() {
        return System.getProperty("app.home") != null;  // Started with appassembly
    }

    Server getServer() {
        return server;
    }
}


