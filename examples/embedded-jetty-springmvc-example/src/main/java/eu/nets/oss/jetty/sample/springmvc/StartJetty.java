package eu.nets.oss.jetty.sample.springmvc;

import eu.nets.oss.jetty.ContextPathConfig;
import eu.nets.oss.jetty.StaticConfig;
import eu.nets.oss.jetty.StdoutRedirect;
import eu.nets.oss.jetty.sample.springmvc.config.ApplicationConfiguration;
import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class StartJetty {
    public static void main(String[] args) throws Exception {

        routeLoggingThroughSlf4j();

        //EmbeddedJettyBuilder builder = EmbeddedJettyBuilder.create(config());

        //EmbeddedJettyBuilder.ServletContextHandlerBuilder<ServletContextHandler> servletContextHandler = builder.withAccessLog().createRootServletContextHandler("");

        ApplicationContext ctx = new AnnotationConfigApplicationContext(ApplicationConfiguration.class);
        AnnotationConfigWebApplicationContext webCtx = new AnnotationConfigWebApplicationContext();
        webCtx.setParent(ctx);
        webCtx.setConfigLocation("eu.nets.oss.jetty.sample.springmvc.config.MvcConfiguration");

        /*servletContextHandler.setResourceHandler(new ClasspathResourceHandler("/webapp", false));

        HandlerBuilder<ContextHandler> rootContextHandler = builder.createRootContextHandler("");

        DispatcherServlet servlet = new DispatcherServlet(webCtx);
        servletContextHandler.addServlet(servlet).mountAtPath("*.htm");

        builder.run();*/

        System.setProperty("org.apache.jasper.compiler.disablejsr199", "false");

        Server server = new Server(8080);

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new DispatcherServlet(webCtx)), "/");
        context.addEventListener(new ContextLoaderListener(webCtx));
        context.setResourceBase(new ClassPathResource("webapp").getURI().toString());

        server.setHandler(context);

        JettyJasperInitializer sci = new JettyJasperInitializer();
        ServletContainerInitializersStarter sciStarter = new ServletContainerInitializersStarter(context);

        ContainerInitializer initializer = new ContainerInitializer(sci, null);
        List<ContainerInitializer> initializers = new ArrayList<ContainerInitializer>();

        initializers.add(initializer);

        context.setAttribute("org.eclipse.jetty.containerInitializers", initializers);
        context.addBean(sciStarter, true);

        context.setAttribute("javax.servlet.context.tempdir", getScratchDir());


        context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());

        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], StartJetty.class.getClassLoader());
        context.setClassLoader(jspClassLoader);

        ServletHolder holderJsp = new ServletHolder("jsp", JspServlet.class);

        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("keepgenerated", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.8");
        holderJsp.setInitParameter("compilerSourceVM", "1.8");
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");

        context.addServlet(holderJsp, "*.jsp");

        server.start();
        server.join();
    }

    private static File getScratchDir() throws IOException
        {
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            File scratchDir = new File(tempDir.toString(), "embedded-jetty-jsp");

            if (!scratchDir.exists())
            {
                if (!scratchDir.mkdirs())
                {
                    throw new IOException("Unable to create scratch directory: " + scratchDir);
                }
            }
            return scratchDir;
        }

    private static ServletHolder defaultServletHolder(URI baseUri) {
        ServletHolder holderDefault = new ServletHolder("default", DefaultServlet.class);

        holderDefault.setInitParameter("resourceBase", baseUri.toASCIIString());
        holderDefault.setInitParameter("dirAllowed", "true");
        return holderDefault;
    }

    private static void routeLoggingThroughSlf4j() {
        StdoutRedirect.tieSystemOutAndErrToLog();
        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
    }

    private static ContextPathConfig config() {
        return new StaticConfig("/", 8080);
    }

}
