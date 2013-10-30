package eu.nets.oss.jetty;

import static com.google.common.base.Throwables.propagate;
import static java.awt.Desktop.getDesktop;
import static java.lang.String.format;
import static java.net.InetAddress.getLocalHost;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.IPAccessHandler;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.JavaUtilLog;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.thread.QueuedThreadPool;


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
    private int headerBufferSize = 8192; // SiteMinder uses lots of HEAD space
    List<HandlerBuilder> handlers = new ArrayList<HandlerBuilder>();

    /**
     *Create a new builder.
     *
     * @param context The context defining the root path and port of the application
     * @param devMode true to run in development mode, which normally caches less content.
     */
    public EmbeddedJettyBuilder(ContextPathConfig context, boolean devMode) {
        this.contextPath = context.getContextPath();
        this.port = context.getPort();
        this.devMode = devMode;
        server = createServer(port, devMode, Boolean.getBoolean("embedded.jetty.daemon"));
    }

    public static class HandlerBuilder<T extends Handler> {
        private final T handler;

        public HandlerBuilder(T handler) {
            this.handler = handler;
        }

        public T getHandler() {
            return handler;
        }

        public HandlerBuilder<T> setResourceHandler(ResourceHandler resourceHandler) {
            ((ContextHandler) handler).setHandler(resourceHandler);
            return this;
        }
    }

    public class ServletContextHandlerBuilder extends HandlerBuilder<ServletContextHandler> {
        private final ServletContextHandler handler;

        public ServletContextHandlerBuilder(ServletContextHandler handler) {
            super(handler);
            this.handler = handler;
            setHttpCookieOnly(true);
        }

        public ServletHolderBuilder addServlet(Servlet servlet) {
            return new ServletHolderBuilder(this, servlet);
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

        public ServletContextHandlerBuilder addFilter(Filter filter, String pathSpec, EnumSet<DispatcherType> dispatches) {
            FilterHolder fh = new FilterHolder(filter);
            handler.addFilter(fh, pathSpec, dispatches);
            return this;
        }

        public ServletContextHandlerBuilder addFilterHolder(FilterHolder filterHolder, String pathSpec, EnumSet<DispatcherType> dispatches) {
            handler.addFilter(filterHolder, pathSpec, dispatches);
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

        public ServletContextHandlerBuilder setResourceHandler(ResourceHandler resourceHandler) {
            handler.setHandler(resourceHandler);
            return this;
        }

    }

    public class ServletHolderBuilder {
        private final ServletHolder sh;
        private final EmbeddedJettyBuilder.ServletContextHandlerBuilder servletContext;

        public ServletHolderBuilder(ServletContextHandlerBuilder servletContext, Servlet servlet) {
            sh = new ServletHolder(servlet);
            this.servletContext = servletContext;
        }

        public ServletHolderBuilder mountAtPath(String pathSpec) {
            this.servletContext.handler.addServlet(sh, pathSpec);
            return this;

        }

        public ServletHolderBuilder setServletName(String name){
            sh.setName( name );
            return this;
        }

        public ServletHolderBuilder setInitParameter(String param, String value) {
            sh.setInitParameter(param, value);
            return this;

        }
    }

    private void setPath(ContextHandler handler, String usePath) {
        Logger.info(this.getClass(), ">>>> Context handler added at " + usePath + " <<<<");
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
        return new ServletContextHandlerWithExceptions(contextPath, startupExceptions);
    }

    private void failIfPortIsTaken(int port) {
        // SelectChannelConnector allows multiple processes to
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Port " + port + " is already in use");
        }


    }

    private Server createServer(int port, boolean devMode, boolean daemon) {
        // The NIO connectors permit one process to queue up for the socket.
        // (So when the owning process terminates, the new takes over)
        // While there may be a decent production case for this, it is error prone on local workstation.
        // todo: Determine if we need to support this for production purposes.

        failIfPortIsTaken(port);
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
        queuedThreadPool.setDaemon(daemon);
        queuedThreadPool.setName("embedded-jetty");
        Server server = new Server(queuedThreadPool);

        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setRequestHeaderSize(headerBufferSize);
        http_config.setResponseHeaderSize(headerBufferSize);

        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(http_config));
        connector.setPort(port);

        if (devMode) {
            connector.setIdleTimeout(1000000);
        } else {
            connector.setIdleTimeout(30000);
            server.setStopAtShutdown(true);
            server.setStopTimeout(2000);
        }
        connector.setIdleTimeout(10000000);

        //connector.setSoLingerTime(-1);
        server.addConnector(connector);
        return server;
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

    public void justStartJetty() {
        this.initTime = System.currentTimeMillis();
        buildJetty();
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startJetty() {
        Logger.info(this.getClass(), "************************** Server starting: {} **************************",
                new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS]").format(new Date()));
        try {
            justStartJetty();
            verifyServerStartup();
        } catch (Exception e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            propagate(e);
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
            getLoggerS().info("Unable to retrieve hostname", e);
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

    protected static JavaUtilLog getLoggerS() {
        return new JavaUtilLog(EmbeddedJettyBuilder.class.getName());
    }

    protected JavaUtilLog getLogger() {
        return new JavaUtilLog(this.getClass().getName());
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
     * Creates a standard resource handler, that can be attached using setResourceHandler
     */
    public ClasspathResourceHandler createWebAppClasspathResourceHandler() {
        boolean useCaches = !devMode;
        ClasspathResourceHandler classpathResourceHandler = new ClasspathResourceHandler("/webapp", useCaches);
        return classpathResourceHandler;
    }

    /**
     * @return true if the current process has been started with appassambler
     */
    public static boolean isStartedWithAppassembler() {
        final String[] appAssemblerProperties = {
                "app.home",
                "app.name",
                "app.repo",
        };
        for (String appAssemblerProperty : appAssemblerProperties) {
            if (System.getProperty(appAssemblerProperty) != null) {
                return true;
            }
        }
        return false;
    }

    Server getServer() {
        return server;
    }

    public void addLifecycleListener(LifeCycle.Listener listener){
        server.addLifeCycleListener(listener);
    }


    public void setHeaderBufferSize(int headerBufferSize) {
        this.headerBufferSize = headerBufferSize;
    }
}


