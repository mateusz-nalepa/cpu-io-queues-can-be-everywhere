package com.nalepa.demo.httpclient

import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class HttpClientFactory(
    private val restClientBuilder: RestClient.Builder,
) {

    fun createRestClient(): RestClient {

        val connectionManager = PoolingHttpClientConnectionManager()
        connectionManager.maxTotal = 1000
        connectionManager.defaultMaxPerRoute = 1000
        val httpClient = HttpClients.custom().setConnectionManager(connectionManager).build()
        val requestFactory = HttpComponentsClientHttpRequestFactory(httpClient)

        return restClientBuilder
            .requestFactory(requestFactory)
            .build()
    }

}