package eu.nets.oss.jetty;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.ws.transport.http.MessageDispatcherServlet;

/**
 * Spring-ws builder code
 *
 * Kept in a separate class so we can keep the dependency on spring optional
 *
 * @author Kristian Rosenvold
 */
public class EmbeddedSpringWsBuilder {

    /**
     * Creates a spring-ws message dispatcher servlet
     * @param contextConfigLocation The spring configuration classes
     * @return A message dispatcher servlet based on the supplied configuration
     */
    public static MessageDispatcherServlet createMessageDispatcherServlet(Class... contextConfigLocation) {
        StringBuilder items = new StringBuilder();
        for (Class aClass : contextConfigLocation) {
            items.append( aClass.getName());
            items.append(",");
        }
        MessageDispatcherServlet messageDispatcherServlet = new MessageDispatcherServlet();
        messageDispatcherServlet.setContextClass(AnnotationConfigWebApplicationContext.class);
        messageDispatcherServlet.setContextConfigLocation(removeEnd(items.toString(), "," ));
        messageDispatcherServlet.setTransformWsdlLocations(true);
        return messageDispatcherServlet;
    }

    public static String removeEnd(String str, String remove) {
        if (str == null || str.isEmpty() || remove == null || remove.isEmpty()) {
            return str;
        }
        if (str.endsWith(remove)) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }

}
