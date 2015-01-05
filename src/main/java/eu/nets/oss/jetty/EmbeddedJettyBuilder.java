package eu.nets.oss.jetty;

import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private Server server;
    private final String contextPath;
    private final int port;
    private final boolean devMode;
    private IPAccessHandler secureWrap;
    private final LinkedList<Exception> startupExceptions = new LinkedList<>();
    private long initTime;
    private int headerBufferSize = 8192; // SiteMinder uses lots of HEAD space
    List<HandlerBuilder> handlers = new ArrayList<>();
    private boolean shouldExportMBeans;
    private QueuedThreadPool queuedThreadPool;
    private boolean shouldSendVersionNumber;
    private boolean useFileMappedBuffer;

    private HttpsServerConfig httpsServerConfig;

    /**
     *Create a new builder.
     *
     * @param context The context defining the root path and port of the application
     * @param devMode true to run in development mode, which normally caches less content.
     */
    public EmbeddedJettyBuilder(ContextPathConfig context, boolean devMode) {
        this(context, devMode, 8192);
    }

    public EmbeddedJettyBuilder(ContextPathConfig context, boolean devMode, int headerBufferSize) {
        this.headerBufferSize = headerBufferSize;
        this.contextPath = context.getContextPath();
        this.port = context.getPort();
        this.devMode = devMode;

        // Disable useFileMappedBuffer in development mode.
        useFileMappedBuffer = !devMode;
    }

    public EmbeddedJettyBuilder exportMBeans() {
        this.shouldExportMBeans = true;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void addWhiteList(Iterable<String> allowedIps) {
        secureWrap = new IPAccessHandler();
        for (String allowedIp : allowedIps) {
            secureWrap.addWhite(allowedIp);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public EmbeddedJettyBuilder sendVersionNumber() {
        this.shouldSendVersionNumber = true;
        return this;
    }


    public EmbeddedJettyBuilder setHttpsServerConfig(HttpsServerConfig httpsConfig) {
        this.httpsServerConfig = httpsConfig;
        return this;
    }


    public EmbeddedJettyBuilder withThreadPool(QueuedThreadPool queuedThreadPool) {
        this.queuedThreadPool = queuedThreadPool;
        return this;
    }

    public EmbeddedJettyBuilder withUseFileMappedBuffer(boolean useFileMappedBuffer) {
        this.useFileMappedBuffer = useFileMappedBuffer;
        return this;
    }

    public static class ServletContextHandlerBuilder<H extends ServletContextHandler> extends HandlerBuilder<H> {

        private final ServletContextHandler handler;

        public ServletContextHandlerBuilder(H handler) {
            super(handler);
            this.handler = handler;
            setHttpCookieOnly(true);
        }

        public ServletHolderBuilder addServlet(Servlet servlet) {
            return new ServletHolderBuilder<>(this, servlet);
        }

        public ServletContextHandlerBuilder<H> setHttpCookieOnly(boolean httpCookieOnly) {
            handler.getSessionHandler().getSessionManager().getSessionCookieConfig().setHttpOnly(httpCookieOnly);
            return this;
        }

        /**
         * Adds an Event Listener to this servlet context, typically some implementation of ServletContextListener
         *
         * @param eventListener The event listener to add
         * @return this builder
         */
        public ServletContextHandlerBuilder<H> addEventListener(EventListener eventListener) {
            handler.addEventListener(eventListener);
            return this;
        }

        public FilterBuilder<ServletContextHandlerBuilder<H>> addFilter(Filter filter, String pathSpec, EnumSet<DispatcherType> dispatches) {
            FilterHolder fh = new FilterHolder(filter);
            handler.addFilter(fh, pathSpec, dispatches);
            return new FilterBuilder<>(fh, this);
        }

        public FilterBuilder<ServletContextHandlerBuilder<H>> addFilter(Class<? extends Filter> filter, String pathSpec, EnumSet<DispatcherType> dispatches) {
            FilterHolder fh = new FilterHolder(filter);
            handler.addFilter(fh, pathSpec, dispatches);
            return new FilterBuilder<>(fh, this);
        }

        public FilterBuilder<ServletContextHandlerBuilder<H>> addFilter(FilterHolder filterHolder, String pathSpec, EnumSet<DispatcherType> dispatches) {
            handler.addFilter(filterHolder, pathSpec, dispatches);
            return new FilterBuilder<>(filterHolder, this);
        }

        /**
         * @deprecated Use {@link #addFilter(javax.servlet.Filter, String, java.util.EnumSet)} instead.
         */
        public ServletContextHandlerBuilder<H> addFilterHolder(FilterHolder filterHolder, String pathSpec, EnumSet<DispatcherType> dispatches) {
            handler.addFilter(filterHolder, pathSpec, dispatches);
            return this;
        }

        public ServletContextHandlerBuilder<H> setSecurityHandler(SecurityHandler securityHandler) {
            handler.setSecurityHandler(securityHandler);
            return this;
        }

        public ServletContextHandlerBuilder<H> setClassLoader(ClassLoader classLoader) {
            handler.setClassLoader(classLoader);
            return this;
        }

        public ServletContextHandlerBuilder<H> setResourceHandler(ResourceHandler resourceHandler) {
            handler.setHandler(resourceHandler);
            return this;
        }
    }

    public static class WebAppContextBuilder extends ServletContextHandlerBuilder<WebAppContext> {
        public WebAppContextBuilder(WebAppContext handler, Resource baseResource) {
            super(handler);
            handler.setBaseResource(baseResource);
        }
    }

    public static class ServletHolderBuilder<H extends ServletContextHandler> {
        private final ServletHolder sh;
        private final ServletContextHandlerBuilder<H> servletContext;

        public ServletHolderBuilder(ServletContextHandlerBuilder<H> servletContext, Servlet servlet) {
            sh = new ServletHolder(servlet);
            this.servletContext = servletContext;
        }

        public ServletHolderBuilder mountAtPath(String pathSpec) {
            this.servletContext.handler.addServlet(sh, pathSpec);
            return this;
        }

        public ServletHolderBuilder setServletName(String name) {
            sh.setName(name);
            return this;
        }

        public ServletHolderBuilder setInitParameter(String param, String value) {
            sh.setInitParameter(param, value);
            return this;
        }

        public ServletContextHandlerBuilder<H> done() {
            return servletContext;
        }

        public ServletHolderBuilder setMultipartConfig(MultipartConfigElement multipartConfig) {
            sh.getRegistration().setMultipartConfig(multipartConfig);
            return this;
        }
    }

    private void setPath(ContextHandler handler, String usePath) {
        handler.setContextPath(usePath);
    }

    /**
     * Creates a HandlerBuilder that is mounted on top of the root path of this builder
     *
     * @param subPath The sub-path that will be appended, starting with a slash, or just an empty string for no sub-path
     * @return A handler builder. This can not be used for servlets, see #createRootServletContextHandler
     */
    public HandlerBuilder<ContextHandler> createRootContextHandler(String subPath) {
        ContextHandler contextHandler = new ContextHandler();
        HandlerBuilder<ContextHandler> e = new HandlerBuilder<>(contextHandler);
        String usePath = contextPath + subPath;
        setPath(contextHandler, usePath);
        handlers.add(e);
        return e;
    }

    /**
     * Creates a ServletContextHandlerBuilder that is mounted on top of the root path of this builder
     *
     * @param subPath The sub-path that will be appended, starting with a slash, or just an empty string for no sub-path
     * @return A handler builder
     */
    public ServletContextHandlerBuilder<ServletContextHandler> createRootServletContextHandler(String subPath) {
        return createRootServletContextHandlerInternal(subPath, null);
    }

    public ServletContextHandlerBuilder<WebAppContext> createRootWebAppContext(String subPath, Resource baseResource) {
        return createRootWebAppContext(subPath, baseResource, null);
    }

    /**
     * Example requestLogger:
     * <pre>
     * NCSARequestLog requestLog = new NCSARequestLog("logs/my-app-jetty-web-yyyy_mm_dd.request.log");
     * requestLog.setRetainDays(90);
     * requestLog.setAppend(true);
     * requestLog.setExtended(false);
     * requestLog.setLogTimeZone("Europe/Oslo"); // or GMT
     * </pre>
     * https://wiki.eclipse.org/Jetty/Howto/Configure_Request_Logs
     */
    public ServletContextHandlerBuilder createRootServletContextHandler(String subPath, RequestLog requestLogger) {
        if (requestLogger == null) {
            throw new RuntimeException("RequestLogger cannot be null");
        }
        return createRootServletContextHandlerInternal(subPath, requestLogger);
    }

    private ServletContextHandlerBuilder<ServletContextHandler> createRootServletContextHandlerInternal(String subPath, RequestLog requestLogger) {
        ServletContextHandler handler = getServletContextHandler();
        ServletContextHandlerBuilder<ServletContextHandler> e = new ServletContextHandlerBuilder<>(handler);
        String usePath = contextPath + subPath;
        setPath(e.getHandler(), usePath);
        handlers.add(requestLogger == null ? e : wrapWithRequestLogger(e, requestLogger));
        return e;
    }

    private WebAppContextBuilder createRootWebAppContext(String subPath, Resource baseResource, RequestLog requestLogger) {
        WebAppContext handler = new WebAppContext();
        handler.setContextPath(subPath);
        WebAppContextBuilder builder = new WebAppContextBuilder(handler, baseResource);
        String usePath = contextPath + subPath;
        setPath(builder.getHandler(), usePath);
        handlers.add(requestLogger == null ? builder : wrapWithRequestLogger(builder, requestLogger));
        return builder;
    }

    private HandlerBuilder<RequestLogHandler> wrapWithRequestLogger(ServletContextHandlerBuilder e, RequestLog requestLogger) {
        RequestLogHandler handler = new RequestLogHandler();
        handler.setHandler(e.handler);
        handler.setRequestLog(requestLogger);
        return new HandlerBuilder<>(handler);
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

        long idleTimeOut = devMode ? 1000000 : 30000;

        failIfPortIsTaken(port);
        if (queuedThreadPool == null) {
            queuedThreadPool = new QueuedThreadPool();
        }
        queuedThreadPool.setDaemon(daemon);
        queuedThreadPool.setName("embedded-jetty");
        Server server = new Server(queuedThreadPool);
        HttpConfiguration http_config = new HttpConfiguration();

        http_config.setSendServerVersion(shouldSendVersionNumber);
        http_config.setRequestHeaderSize(headerBufferSize);
        http_config.setResponseHeaderSize(headerBufferSize);

        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(http_config));
        connector.setPort(port);
        connector.setIdleTimeout(idleTimeOut);

        if (!devMode) {
            server.setStopAtShutdown(true);
            server.setStopTimeout(2000);
        }

        server.addConnector(connector);

        Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);

        // Enable annotation processing if the class is available

        boolean hasAnnotationConfiguration = false;
        try {
            Class.forName("org.eclipse.jetty.annotations.AnnotationConfiguration");
            classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");
            hasAnnotationConfiguration = true;
            Logger.debug(getClass(), "Annotation processing is enabled.");
        } catch (ClassNotFoundException e) {
            Logger.debug(getClass(), "Annotation processing is not enabled, missing dependency on jetty-annotations.");
        }

        try {
            Class.forName("org.eclipse.jetty.apache.jsp.JettyJasperInitializer");
            if (!hasAnnotationConfiguration) {
                Logger.debug(getClass(), "JSP support is not enabled, annotation processing is required.");
            } else {
                Logger.debug(getClass(), "JSP support is enabled with Apache Jasper.");
            }
        } catch (ClassNotFoundException e) {
            Logger.debug(getClass(), "JSP support is not enabled, add a dependency on apache-jsp.");
        }

        if (!useFileMappedBuffer) {
            classlist.add(DisableFileMappedBufferConfiguration.class.getName());
        }



        if (httpsServerConfig != null) {
            HttpConfiguration https_config = new HttpConfiguration(http_config);
            https_config.setSecureScheme(HttpScheme.HTTPS.asString());
            https_config.setSecurePort(httpsServerConfig.getHttpsServerPort());
            https_config.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory sslContextFactory = httpsServerConfig.createSslContextFactory();

            ServerConnector sslConnector = new ServerConnector(
                    server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(https_config)
            );

            sslConnector.setPort(httpsServerConfig.getHttpsServerPort());
            sslConnector.setIdleTimeout(idleTimeOut);

            server.addConnector(sslConnector);
        }



        return server;
    }

    /**
     * This is a hack to get the default servlet to not use file mapping buffers when serving files. The effect is that
     * Jetty reloads the file on every read but it also does not lock the file which is good for development.
     */
    public static class DisableFileMappedBufferConfiguration extends AbstractConfiguration {
        @Override
        public void configure(WebAppContext context) throws Exception {
            ServletContextHandler.Decorator useFileMappedBuffer = new ServletContextHandler.Decorator() {
                @Override
                public <T> T decorate(T o) {
                    if (o instanceof DefaultServlet) {
                        Class<T> klass = (Class<T>) o.getClass();

                        return klass.cast(new DefaultServlet() {
                            @Override
                            public String getInitParameter(String name) {
                                if (name.equals("useFileMappedBuffer")) {
                                    return "false";
                                }

                                return super.getInitParameter(name);
                            }
                        });
                    }
                    return o;
                }

                @Override
                public void destroy(Object o) {
                }
            };

            context.addDecorator(useFileMappedBuffer);
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
        if (shouldExportMBeans) {
            JettyJmx.exportMBeans(server);
        }
        return server;
    }

    public EmbeddedJettyBuilder createServer() {
        this.server = createServer(port, devMode, Boolean.getBoolean("embedded.jetty.daemon"));
        return this;
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
            final String hostAddress = failSafeGetHostname();
            new Socket(hostAddress, port).close();
            throw new IllegalArgumentException(String.format("A running server was found on '%s', port '%d'",
                    hostAddress, port));
        } catch (IOException e) {
            //
        }
    }

    private static String failSafeGetHostname() throws UnknownHostException {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            Logger.info(EmbeddedJettyBuilder.class, "Unable to retrieve hostname", e);
            return "localhost";
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void verifyServerStartup() {
        for (Handler handler : server.getHandlers()) {
            if (!verifyStartup(handler)) {
                System.exit(-1);
            }
        }

        // This can probably be removed after verifyStartup() was made, but I'm not sure how to check it.
        if (!startupExceptions.isEmpty()) {
            Exception exception = startupExceptions.peekFirst();
            Logger.warn(getClass(), format("Errors during startup. The first is %s: %s", exception.getClass().getName(), exception.getMessage()));
            System.exit(-1);
        }

        long startupTime = (System.currentTimeMillis() - initTime);
        String ip = "0.0.0.0";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignore) {/* care face */
        }
        Logger.info(getClass(), format("Server started on http://%s:%d%s in %dms", ip, port, contextPath, startupTime));
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private boolean verifyStartup(Handler handler) {
        if (handler instanceof HandlerContainer) {
            HandlerContainer list = (HandlerContainer) handler;
            for (Handler h : list.getHandlers()) {
                if (!verifyStartup(h)) {
                    return false;
                }
            }
        }

        if (handler instanceof WebAppContext) {
            WebAppContext webAppContext = (WebAppContext) handler;
            Throwable exception = webAppContext.getUnavailableException();

            if (exception != null) {
                Logger.warn(getClass(), format("Errors during startup. The first is %s: %s. See the log for details.", exception.getClass().getName(), exception.getMessage()));
                return false;
            }
        } else if (handler instanceof ServletHandler) {
            ServletHandler servletHandler = (ServletHandler) handler;

            if (!servletHandler.isAvailable()) {
                Logger.warn(getClass(), format("Errors during startup of servlet. See the log for details."));
                return false;
            }
        }

        return true;
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
        return new ClasspathResourceHandler("/webapp", useCaches);
    }

    /**
     * @return true if the current process has been started with appassembler.
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

    public Server getServer() {
        return server;
    }

    public void addLifecycleListener(LifeCycle.Listener listener) {
        server.addLifeCycleListener(listener);
    }

    public static class FilterBuilder<O> {
        private final FilterHolder filterHolder;
        private final O owner;

        public FilterBuilder(FilterHolder filterHolder, O owner) {
            this.filterHolder = filterHolder;
            this.owner = owner;
        }

        public FilterBuilder<O> addInitParameter(String key, String value) {
            filterHolder.setInitParameter(key, value);
            return this;
        }

        public O done() {
            return owner;
        }
    }
}
