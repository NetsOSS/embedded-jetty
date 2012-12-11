package eu.nets.utils.jetty.embedded;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.ws.transport.http.MessageDispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

/**
 * Spring initialization code
 *
 * Kept in a separate class so we can keep the dependency on spring optional
 *
 * @author Kristian Rosenvold
 */
public class EmbeddedSpringBuilder {
    public static ContextLoaderListener createSpringContextLoader(final WebApplicationContext webApplicationContext) {
        return new ContextLoaderListener() {
            @SuppressWarnings("unchecked")
            @Override
            protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
                return webApplicationContext;
            }
        };
    }

    public static WebApplicationContext createApplicationContext(final Class<?> classes, final String displayName) {
        return new AnnotationConfigWebApplicationContext(){
            {
                setDisplayName(displayName);
                register( classes);
                super.refresh();
            }
            @Override
            public void refresh() throws BeansException, IllegalStateException {
                // We do this to avoid the spring stuff re-initializing the container once the servlet context is loaded
            }
        };
    }

    public static MessageDispatcherServlet createMessageDispatcherServlet(Class... contextConfigLocation) {
        StringBuilder items = new StringBuilder();
        for (Class aClass : contextConfigLocation) {
            items.append( aClass.getName());
            items.append(",");
        }
        MessageDispatcherServlet messageDispatcherServlet = new MessageDispatcherServlet();
        messageDispatcherServlet.setContextClass(AnnotationConfigWebApplicationContext.class);
        messageDispatcherServlet.setContextConfigLocation(StringUtils.removeEnd(items.toString(), "," ));
        messageDispatcherServlet.setTransformWsdlLocations(true);
        return messageDispatcherServlet;
    }


}
