package eu.nets.oss.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;

import eu.nets.oss.jetty.ContextPathConfig;
import eu.nets.oss.jetty.EmbeddedJettyBuilder;
import eu.nets.oss.jetty.StaticConfig;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.EnumSet;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

/**
 * @author Kristian Rosenvold
 */
public class EmbeddedJettyBuilderTest {

    private final String contextPath = "/abc";
    private final int port = 8099;

    @Test
    public void testCreateRootContextHandler() throws Exception {
        EmbeddedJettyBuilder builder = getBuilder();
        String path = "/def";
        builder.createRootContextHandler(path);
        assertEquals(1, builder.handlers.size());
        ContextHandler handler = getFirstHandler(builder.buildJetty());
        assertEquals(contextPath + path, handler.getContextPath());
    }


    @Test
    public void testCreateRootServletContextHandler() throws Exception {
        EmbeddedJettyBuilder builder = getBuilder();
        String path = "/deb";
        builder.createRootServletContextHandler( path );
        assertEquals(1, builder.handlers.size());
        ServletContextHandler handler = getFirstHandler(builder.buildJetty());
        assertEquals(contextPath + path, handler.getContextPath());
    }

    @Test
    public void testAddFilter() throws Exception {
        EmbeddedJettyBuilder builder = getBuilder();
        String path = "/deb";
        EmbeddedJettyBuilder.ServletContextHandlerBuilder hdl = builder.createRootServletContextHandler( path );
        TestFilter filter = new TestFilter();
        hdl.addFilter( filter, "/foo", EnumSet.of( DispatcherType.REQUEST ) );
        ServletContextHandler handler = getFirstHandler( builder.buildJetty() );
        FilterHolder filterHolder = handler.getServletHandler().getFilters()[0];
        assertTrue(  filter == filterHolder.getFilter());
    }

    @Test
    public void testAddFilterHolder() throws Exception {
        EmbeddedJettyBuilder builder = getBuilder();
        String path = "/deb";
        EmbeddedJettyBuilder.ServletContextHandlerBuilder hdl = builder.createRootServletContextHandler( path );
        TestFilter filter = new TestFilter();
        FilterHolder fh = new FilterHolder( filter  );
        hdl.addFilterHolder( fh, "/foo", EnumSet.of( DispatcherType.REQUEST ) );
        ServletContextHandler handler = getFirstHandler( builder.buildJetty() );
        FilterHolder filterHolder = handler.getServletHandler().getFilters()[0];
        assertTrue(  fh == filterHolder);
        assertTrue(  filter == filterHolder.getFilter());
    }

    private EmbeddedJettyBuilder.ServletContextHandlerBuilder createStdContext( EmbeddedJettyBuilder builder )
    {
        String path = "/deb";
        return builder.createRootServletContextHandler( path );
    }

    @Test
    public void testAddWhiteList() throws Exception {
        // todo: Make test
    }

    @Test
    public void testAddKeystore() throws Exception {
        // todo: Make test
    }

    @Test
    public void testStartStopJetty() throws Exception {

        EmbeddedJettyBuilder builder = getBuilder();
        builder.justStartJetty();
        // Maybe assert that port is taken
        try {
            new ServerSocket(port);
            fail("Should be in use");
        } catch (BindException e) {
            builder.stopJetty();
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.close();
        }
    }

    @Test
    public void testAddHttpAccessLogAtRoot() throws Exception {
        // todo: Make test

    }

    @Test
    public void testGetStartupExceptions() throws Exception {
        // todo: Make test
    }

    @Test
    public void testVerifyServerStartup() throws Exception {
        // todo: Make test
    }

    @Test
    public void testGetConstraintMapping() throws Exception {
        // todo: Make test
    }

    @Test
	    public void testCreateWebAppClasspathResourceHandler() throws Exception {
	        // todo: Make test
	    }

    @Test
    public void testIsStartedWithAppassembler() throws Exception {
        // todo: Make test

    }

    private HandlerList getHandlerList(Server server) {
        assertEquals(1, server.getHandlers().length);
        return (HandlerList) server.getHandlers()[0];
    }


    private EmbeddedJettyBuilder getBuilder() {
        ContextPathConfig config = new StaticConfig(contextPath, port);
        return new EmbeddedJettyBuilder(config, true);
    }

    private <T extends Handler> T getFirstHandler(Server server) {
        //noinspection unchecked
        return (T) getHandlerList(server).getHandlers()[0];
    }

    class TestFilter implements Filter
    {
        public void init( FilterConfig filterConfig )
            throws ServletException
        {
        }

        public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
            throws IOException, ServletException
        {
        }

        public void destroy()
        {
        }
    }
}
