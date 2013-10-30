package eu.nets.oss.jetty.embedded;

import org.eclipse.jetty.servlet.ServletContextHandler;

import java.util.List;

/**
* @author Kristian Rosenvold
*/
class ServletContextHandlerWithExceptions extends ServletContextHandler {
    private final List<Exception> startupExceptions;
    public ServletContextHandlerWithExceptions(String contextPath1, List<Exception> startupExceptions) {
        super(null, contextPath1, ServletContextHandler.SESSIONS);
        this.startupExceptions = startupExceptions;
    }

    @Override
    protected void startContext() throws Exception {
        try {
            super.startContext();
        } catch (Exception e) {
            startupExceptions.add(e);
            throw e;
        }
    }
}
