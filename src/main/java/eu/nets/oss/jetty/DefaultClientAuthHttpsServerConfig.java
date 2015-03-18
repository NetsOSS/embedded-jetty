package eu.nets.oss.jetty;

import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;


public class DefaultClientAuthHttpsServerConfig extends DefaultHttpsServerConfig {

    private boolean httpsServerClientCertificateRequired = true;
    private Resource httpsServerTrustStoreResource;
    private String httpsServerTrustStorePassword;


    public DefaultClientAuthHttpsServerConfig() {
        super();

        String httpsServerTrustStorePathProperty = System.getProperty("https.server.trust.store.path");
        String httpsServerTrustStorePasswordProperty = System.getProperty("https.server.trust.store.password");

        if (httpsServerTrustStorePathProperty == null) { throw new IllegalArgumentException("Must have trust store resource/file defined as system property 'https.server.trust.store.path'."); }
        if (httpsServerTrustStorePasswordProperty == null) { throw new IllegalArgumentException("Must have trust store password defined as system property 'https.server.trust.store.password'."); }

        httpsServerTrustStoreResource = Resource.newClassPathResource(httpsServerTrustStorePathProperty);
        httpsServerTrustStorePassword = httpsServerTrustStorePasswordProperty;

        String httpsServerClientCertificateRequiredProperty = System.getProperty("https.server.client.certificate.required");

        // Default is that client cert IS required.
        if (httpsServerClientCertificateRequiredProperty != null) {
            if (httpsServerClientCertificateRequiredProperty.equalsIgnoreCase("no") || httpsServerClientCertificateRequiredProperty.equalsIgnoreCase("false")) {
                httpsServerClientCertificateRequired = false;
            }
        }


    }


    @Override
    public SslContextFactory createSslContextFactory() {
        SslContextFactory sslContextFactory = super.createSslContextFactory();

        if (httpsServerClientCertificateRequired) {
            sslContextFactory.setNeedClientAuth(true);
        } else {
            sslContextFactory.setWantClientAuth(true);
        }

        sslContextFactory.setTrustStoreResource(httpsServerTrustStoreResource);
        sslContextFactory.setTrustStorePassword(httpsServerTrustStorePassword);

        return sslContextFactory;
    }
}
