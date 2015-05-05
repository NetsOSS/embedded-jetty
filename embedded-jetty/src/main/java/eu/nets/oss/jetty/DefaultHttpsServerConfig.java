package eu.nets.oss.jetty;

import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;


public class DefaultHttpsServerConfig implements HttpsServerConfig {

    private static final String[] DEFAULT_PROTOCOLS = new String[] { "TLSv1.1", "TLSv1.2" };
    private String[] protocols;
    
    int httpsServerPort = -1;
    private Resource httpsServerKeyStoreResource;
    private String httpsServerKeyStorePassword;
    
    
    
    public DefaultHttpsServerConfig() {
        String httpsServerPortProperty = System.getProperty("https.server.port");
        String httpsServerKeyStorePathProperty = System.getProperty("https.server.key.store.path");
        String httpsServerKeyStorePasswordProperty = System.getProperty("https.server.key.store.password");

        if (httpsServerPortProperty == null) {
            throw new IllegalArgumentException("Must have HTTPS Server port defined as system property 'https.server.port'.");
        }
        if (httpsServerKeyStorePathProperty == null) {
            throw new IllegalArgumentException("Must have key store resource/file defined as system property 'https.server.key.store.path'.");
        }
        if (httpsServerKeyStorePasswordProperty == null) {
            throw new IllegalArgumentException("Must have key store password defined as system property 'https.server.key.store.password'.");
        }

        httpsServerPort = Integer.valueOf(httpsServerPortProperty);
        httpsServerKeyStoreResource = Resource.newClassPathResource(httpsServerKeyStorePathProperty);
        httpsServerKeyStorePassword = httpsServerKeyStorePasswordProperty;

        protocols = DEFAULT_PROTOCOLS;
    }
    
    
    public void setProtocols(String[] protocols) {
        this.protocols = protocols;
    }
    

    @Override
    public int getHttpsServerPort() {
        return httpsServerPort;
    }

    @Override
    public SslContextFactory createSslContextFactory() {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStoreResource(httpsServerKeyStoreResource);
        sslContextFactory.setKeyStorePassword(httpsServerKeyStorePassword);

        sslContextFactory.setIncludeProtocols(protocols);
        
        sslContextFactory.setExcludeCipherSuites( // Explicitly disable weak ciphers.
                "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
                "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                "SSL_RSA_WITH_RC4_128_SHA",
                "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
                "TLS_ECDH_RSA_WITH_RC4_128_SHA",
                "SSL_RSA_WITH_RC4_128_MD5"
                
        );

        return sslContextFactory;
    }


}
