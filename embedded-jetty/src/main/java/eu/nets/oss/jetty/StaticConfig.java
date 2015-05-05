package eu.nets.oss.jetty;

/**
 * @author Kristian Rosenvold
 */
public class StaticConfig implements ContextPathConfig {

    private final String contextPath;

    private final int port;

    public StaticConfig(String contextPath, int port) {
        this.contextPath = contextPath;
        this.port = port;
    }

    public String getContextPath() {
        return contextPath;
    }

    public int getPort() {
        return port;
    }
}
