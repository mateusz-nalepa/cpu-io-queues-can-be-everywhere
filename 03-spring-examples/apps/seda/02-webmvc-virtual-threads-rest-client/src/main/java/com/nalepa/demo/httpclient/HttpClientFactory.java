package com.nalepa.demo.httpclient;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpClientFactory {

    private final RestClient.Builder restClientBuilder;

    public HttpClientFactory(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public RestClient createRestClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(200);

        var httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }
}