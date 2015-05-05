package eu.nets.oss.jetty;

import org.springframework.beans.BeansException;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import javax.servlet.ServletContext;

/**
 * Spring initialization code
 * <p/>
 * Kept in a separate class so we can keep the dependency on spring optional
 *
 * @author Kristian Rosenvold
 */
public class EmbeddedSpringBuilder {

    /**
     * Creates a spring context loader listener
     *
     * @param webApplicationContext The web application context to use
     * @return A context loader listener that can be used when starting jetty
     */
    public static ContextLoaderListener createSpringContextLoader(final WebApplicationContext webApplicationContext) {
        return new ContextLoaderListener() {
            @SuppressWarnings("unchecked")
            @Override
            protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
                return webApplicationContext;
            }
        };
    }

    /**
     * Instantiates an application context for javaconfig classes.
     * <p/>
     * This context initializes in a custom manner so it can be passed from the outside into
     * jetty servlet contexts. Since they normally think they are self-contained, they try
     * to do the initialization themselves.
     *
     * @param displayName           The name of the spring context, mostly used when debugging to identify contexts.
     * @param contextConfigLocation The spring context config locations
     * @return A fully configured web application context
     */
    public static WebApplicationContext createApplicationContext(final String displayName, final Class... contextConfigLocation) {
        return new AnnotationConfigWebApplicationContext() {
            boolean refreshed = false;

            {
                setDisplayName(displayName);
                register(contextConfigLocation);
            }

            @Override
            public void refresh() throws BeansException, IllegalStateException {
                if (!refreshed) {
                    // We do this to avoid the spring stuff re-initializing
                    // the container once the servlet context is loaded
                    refreshed = true;
                    super.refresh();
                }
            }
        };
    }
}
