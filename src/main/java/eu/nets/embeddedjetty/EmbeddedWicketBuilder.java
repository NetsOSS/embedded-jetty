package eu.nets.embeddedjetty;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketServlet;

import java.util.EventListener;

/**
 * @author Kristian Rosenvold
 */
public class EmbeddedWicketBuilder {

    public static void addWicketHandler(EmbeddedJettyBuilder embeddedJettyBuilder,
                                        String contextPath,
                                        EventListener springContextloader,
                                        Class <? extends WebApplication> wicketApplication,
                                        boolean development){
        EmbeddedJettyBuilder.ServletContextHandlerBuilder wicketHandler = embeddedJettyBuilder.createRootServletContextHandler(contextPath)
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .addEventListener(springContextloader);
        addWicket(wicketHandler, wicketApplication, development);
    }

    private static EmbeddedJettyBuilder.ServletContextHandlerBuilder addWicket(EmbeddedJettyBuilder.ServletContextHandlerBuilder handlerBuilder,
                                                                        Class <? extends WebApplication> wicketApplication,
                                                                        boolean development) {
            String filterMapping = "/*";
            WicketServlet wicketServlet = new WicketServlet();
            handlerBuilder.addServlet(wicketServlet, filterMapping)
                    .setInitParameter("applicationFactoryClassName", org.apache.wicket.spring.SpringWebApplicationFactory.class.getName())
                    .setInitParameter("applicationClassName", wicketApplication.getName())
                    .setInitParameter("filterMappingUrlPattern", filterMapping)
                    .setInitParameter("wicket.configuration",
                            development ? RuntimeConfigurationType.DEVELOPMENT.name() :  RuntimeConfigurationType.DEPLOYMENT.name());
        return handlerBuilder;
    }


}
