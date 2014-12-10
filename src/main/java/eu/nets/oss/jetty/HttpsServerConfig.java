package eu.nets.oss.jetty;

import org.eclipse.jetty.util.ssl.SslContextFactory;

public interface HttpsServerConfig {
    int getHttpsServerPort();
    SslContextFactory createSslContextFactory();
}
