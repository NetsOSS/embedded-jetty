package eu.nets.oss.jetty;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.protocol.http.ContextParamWebApplicationFactory;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.http.WicketServlet;

import java.util.EventListener;

/**
 * @author Kristian Rosenvold
 */
public class EmbeddedWicketBuilder {

    public static EmbeddedJettyBuilder.ServletContextHandlerBuilder addWicketHandler(EmbeddedJettyBuilder.ServletContextHandlerBuilder wicketHandler,
                                                                                     Class<? extends WebApplication> wicketApplication,
                                                                                     boolean development) {
        String pathSpec = "/*";

        WicketServlet wicketServlet = new WicketServlet();
        wicketHandler.addServlet(wicketServlet)
                .mountAtPath(pathSpec)
                .setInitParameter(WicketFilter.APP_FACT_PARAM, org.apache.wicket.spring.SpringWebApplicationFactory.class.getName())
                .setInitParameter(ContextParamWebApplicationFactory.APP_CLASS_PARAM, wicketApplication.getName())
                .setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, pathSpec)
                .setInitParameter("wicket.configuration",
                        development ? RuntimeConfigurationType.DEVELOPMENT.name() : RuntimeConfigurationType.DEPLOYMENT.name());

        return wicketHandler;
    }

    /**
     * Note - if the root servlet context handler has already been created, this does not seem to work with jetty 9
     */
    public static EmbeddedJettyBuilder.ServletContextHandlerBuilder addWicketHandler(EmbeddedJettyBuilder embeddedJettyBuilder,
                                                                                     String contextPath,
                                                                                     EventListener springContextloader,
                                                                                     Class<? extends WebApplication> wicketApplication,
                                                                                     boolean development) {

        EmbeddedJettyBuilder.ServletContextHandlerBuilder wicketHandler = embeddedJettyBuilder.createRootServletContextHandler(contextPath)
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .addEventListener(springContextloader);
        return addWicketHandler(wicketHandler, wicketApplication, development);
    }

}
