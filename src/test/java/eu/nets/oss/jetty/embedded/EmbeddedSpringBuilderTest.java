package eu.nets.oss.jetty.embedded;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.ws.transport.http.MessageDispatcherServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import static eu.nets.oss.jetty.embedded.EmbeddedSpringBuilder.*;
import static eu.nets.oss.jetty.embedded.EmbeddedSpringWsBuilder.createMessageDispatcherServlet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Kristian Rosenvold
 */
public class EmbeddedSpringBuilderTest {

    @Test
    public void testCreateApplicationContext() throws Exception {
        WebApplicationContext root = createApplicationContext("root", MyConfig.class);
        ContextLoaderListener springContextLoader = createSpringContextLoader(root);
        ServletContext sc = new MockServletContext();
        springContextLoader.initWebApplicationContext(sc);
        checkContext(root);

    }

    @Test
    public void testCreateMessageDispatcherServlet() throws Exception {
        MessageDispatcherServlet mds = createMessageDispatcherServlet(MyConfig.class);
        mds.init(createMockServletConfig());
        WebApplicationContext root = mds.getWebApplicationContext();
        checkContext(root);

    }

    private ServletConfig createMockServletConfig() {
        return new MockServletConfig();
    }

    @Test
    public void testCreateSpringContextLoader() throws Exception {
        WebApplicationContext root = createApplicationContext("root", MyConfig.class);
        ContextLoaderListener springContextLoader = createSpringContextLoader(root);
        // Maybe create a mock context an init, unsure what we can get from that
        assertNotNull( springContextLoader);
    }


    private void checkContext(WebApplicationContext root) {
        Fud bean = root.getBean(Fud.class);
        assertEquals("baZ!", bean.getBaz());
    }

    public interface Fud {
      String getBaz();
    }
    @SuppressWarnings("UnusedDeclaration")
    @Configuration
    public static class MyConfig {
        @Bean
        public Fud fud(){
            return new Fud(){
                public String getBaz() {
                    return "baZ!";
                }
            };
        }
    }
}
