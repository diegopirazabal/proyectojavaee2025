package com.hcen.periferico.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Proveedor centralizado de Apache HttpClient 5 con SSL bypass
 * NOTA: Solo para desarrollo. En producción debe usarse un truststore apropiado.
 */
@ApplicationScoped
public class HttpClientProvider {
    
    private static final Logger LOGGER = Logger.getLogger(HttpClientProvider.class.getName());
    
    /**
     * Produce un CloseableHttpClient (Apache HttpClient 5) configurado con SSL bypass
     * Este cliente acepta todos los certificados SSL sin validación
     */
    @Produces
    @ApplicationScoped
    public CloseableHttpClient createHttpClient() {
        try {
            // TrustManager que acepta todos los certificados
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            
            // Configurar SSLContext con el TrustManager que acepta todo
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // Crear SSLConnectionSocketFactory con NoopHostnameVerifier
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE
            );
            
            // Crear connection manager con SSL custom
            HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();
            
            LOGGER.warning("HttpClient (Apache 5) configurado con SSL bypass - SIN VALIDACIÓN DE CERTIFICADOS (solo para desarrollo)");
            
            return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "No se pudo configurar SSL permisivo, usando cliente por defecto", e);
            return HttpClients.createDefault();
        }
    }
}
