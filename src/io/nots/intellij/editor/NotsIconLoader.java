package io.nots.intellij.editor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

public class NotsIconLoader {

    private static NotsIconLoader instance = new NotsIconLoader();

    public static NotsIconLoader getInstance() {
        return instance;
    }

    private int requestSocketTimeout = 45000;

    private int connectionRequestTimeout = 30000;

    private int maxTotal = 20;

    private int maxPerRoute = 20;

    private HttpClientBuilder clientBuilder;

    private CloseableHttpClient client;

    private ObjectMapper objectMapper;

    private NotsIconLoader() {

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        PoolingHttpClientConnectionManager cm = getPoolingHttpClientConnectionManager(true);

        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(maxPerRoute);

        this.clientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(requestSocketTimeout).setConnectionRequestTimeout(connectionRequestTimeout).build()).setConnectionManager(cm).setRetryHandler(new DefaultHttpRequestRetryHandler());
        this.client = clientBuilder.build();
    }

    protected PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(boolean disableValidation) {
        if (disableValidation) {
            try {
                SSLContextBuilder builder = SSLContexts.custom();
                builder.loadTrustMaterial(null, (chain, authType) -> true);
                SSLContext sslContext = builder.build();
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());

                Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                        .<ConnectionSocketFactory>create().register("https", sslsf)
                        .build();

                return new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {

            }
        }
        return new PoolingHttpClientConnectionManager();

    }

    public List<NotsIcon> load(String url) {
        try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
            // Check response HTTP status code
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                Notifications.Bus.notify(new Notification("Nots.io", "Failed to load nots from " + url, String.valueOf(response.getStatusLine().getStatusCode()), NotificationType.ERROR));
                return Collections.emptyList();
            }
            return objectMapper.readValue(response.getEntity().getContent(), new TypeReference<List<NotsIcon>>() {
            });
        } catch (IOException e) {
            Notifications.Bus.notify(new Notification("Nots.io", "Failed to load nots from " + url, e.getMessage(), NotificationType.ERROR));
        }
        return Collections.emptyList();
    }
}
