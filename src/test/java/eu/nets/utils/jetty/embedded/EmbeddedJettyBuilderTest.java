package eu.nets.utils.jetty.embedded;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;

import java.net.BindException;
import java.net.ServerSocket;

import static junit.framework.TestCase.assertEquals;
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
        builder.createRootServletContextHandler(path);
        assertEquals(1, builder.handlers.size());
        ServletContextHandler handler = getFirstHandler(builder.buildJetty());
        assertEquals(contextPath + path, handler.getContextPath());
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
    public void testCreateStandardClasspathResourceHandler() throws Exception {
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
}
