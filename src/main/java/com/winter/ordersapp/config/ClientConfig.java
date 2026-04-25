package com.winter.ordersapp.config;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.winter.ordersapp.client.InventoryClient;
import com.winter.ordersapp.client.PaymentClient;

@Configuration
public class ClientConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        // 1. Configure the connection-level timeouts (Same as before)
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(5, TimeUnit.SECONDS))
                .setSocketTimeout(Timeout.of(30, TimeUnit.SECONDS))
                .build();

        // 2. Build the Connection Manager
        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        // 3. Create the HttpClient
        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        // 4. Wrap it in the Spring RequestFactory
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        
        // 5. Build the RestClient using the factory
        return builder
                .requestFactory(factory)
                .build();
    }


    @Bean
    public InventoryClient inventoryClient(RestClient.Builder builder, ServiceProperties props) {
        RestClient restClient = builder
        .baseUrl("http://localhost:8082") // Verify your inventory port!
        .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(InventoryClient.class);
    }
}
