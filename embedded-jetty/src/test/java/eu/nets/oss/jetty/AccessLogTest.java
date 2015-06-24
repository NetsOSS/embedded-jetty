package eu.nets.oss.jetty;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.fail;

public class AccessLogTest {

    private static final String contextPath = "/abc";
    private static final int port = 8199;
    private static Server jetty;
    static final AtomicInteger lastStatus = new AtomicInteger(0);

    @BeforeClass
    public static void startJetty() {
        EmbeddedJettyBuilder builder = getBuilder().createServer();
        EmbeddedJettyBuilder.ServletContextHandlerBuilder handlerBuilder = builder.createRootServletContextHandler("", new RequestLog() {
            @Override
            public void log(Request request, Response response) {
                lastStatus.set(response.getStatus());
            }
        });

        handlerBuilder.addServlet(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                if (req.getRequestURI().contains("/found")) {
                    resp.setStatus(200);
                } else {
                    resp.setStatus(404);
                }
            }
        }).mountAtPath("/test/*");
        jetty = builder.buildJetty();
        try {
            jetty.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void stopJetty() {
        try {
            jetty.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAccessLog() throws Exception {
        URL url = new URL(String.format("http://localhost:%d%s/test/found/abc", port, contextPath));
        try (InputStream is = url.openStream()) {
        }
        Thread.sleep(1000);
        assertEquals(200, lastStatus.get());

        url = new URL(String.format("http://localhost:%d%s/test/notfound/abc", port, contextPath));
        try (InputStream is = url.openStream()) {
            fail("Expected 404");
        } catch (FileNotFoundException exception) {
            assertEquals(404, lastStatus.get());
        }
    }

    private static EmbeddedJettyBuilder getBuilder() {
        ContextPathConfig config = new StaticConfig(contextPath, port);
        return new EmbeddedJettyBuilder(config, true);
    }

}
