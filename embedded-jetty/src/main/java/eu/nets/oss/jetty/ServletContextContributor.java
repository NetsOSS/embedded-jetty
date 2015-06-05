package eu.nets.oss.jetty;

import eu.nets.oss.jetty.EmbeddedJettyBuilder;

public interface ServletContextContributor {

    void contribute(EmbeddedJettyBuilder.ServletContextHandlerBuilder builder);

}
