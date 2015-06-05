package eu.nets.oss.jetty.jersey;

import eu.nets.oss.jetty.EmbeddedJettyBuilder;
import eu.nets.oss.jetty.ServletContextContributor;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

public class StartJetty {
    public static void main(String[] args) {

        EmbeddedJettyBuilder.create()
                .withServletContextContributors(new ServletContextContributor() {
                    @Override
                    public void contribute(EmbeddedJettyBuilder.ServletContextHandlerBuilder builder) {

                        builder.addFilter(ServletContainer.class, "/api/*", EnumSet.of(DispatcherType.REQUEST))
                                .addInitParameter("javax.ws.rs.Application", "eu.nets.oss.jetty.jersey.JerseyExampleApplication");

                        /*builder.addServlet(new ServletContainer())
                                .mountAtPath("/api")
                                .setInitParameter("javax.ws.rs.Application", "eu.nets.oss.jetty.jersey.JerseyExampleApplication")
                                .done();*/
                    }
                })
        .run();

    }
}
