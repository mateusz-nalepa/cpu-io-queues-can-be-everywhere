package com.nalepa.demo.httpclient;

import com.nalepa.demo.common.DummyLogger;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.resources.LoopResources;

import java.time.Duration;

public class HttpClientFactory {

    public static final String WEB_CLIENT_PENDING_REQUEST_TIME = "WEB_CLIENT_PENDING_REQUEST_TIME";

    public record ContextWithStartTime(String index, long startTime) {}

    @Component
    public static class HttpClientFactoryBean {

        private final WebClient.Builder webClientBuilder;
        private final ReactorClientHttpConnectorBuilder reactorClientHttpConnectorBuilder;
        private final MeterRegistry meterRegistry;

        public HttpClientFactoryBean(
                WebClient.Builder webClientBuilder,
                ReactorClientHttpConnectorBuilder reactorClientHttpConnectorBuilder,
                MeterRegistry meterRegistry
        ) {
            this.webClientBuilder = webClientBuilder;
            this.reactorClientHttpConnectorBuilder = reactorClientHttpConnectorBuilder;
            this.meterRegistry = meterRegistry;
        }

        public WebClient createWebClient() {
            return webClientBuilder
                    .filter((request, next) ->
                            next.exchange(request)
                                    .contextWrite(ctx -> {
                                        // executed on server thread
                                        String index = request.headers().getFirst(com.nalepa.demo.common.Constants.DUMMY_INDEX);
                                        if (index != null) {
                                            DummyLogger.log(this, "WORKAROUND: Index: " + index + ". Start " + WEB_CLIENT_PENDING_REQUEST_TIME);
                                            return ctx.put(WEB_CLIENT_PENDING_REQUEST_TIME, new ContextWithStartTime(index, System.nanoTime()));
                                        } else {
                                            return ctx;
                                        }
                                    })
                    )
                    .clientConnector(
                            reactorClientHttpConnectorBuilder
                                    .withReactorResourceFactory(createReactorResourceFactory())
                                    .withHttpClientCustomizer(httpClient ->
                                            httpClient.doOnRequest((httpClientRequest, connection) -> {
                                                // executed on webClient thread
                                                ContextWithStartTime contextWithStartTime = httpClientRequest
                                                        .currentContextView()
                                                        .getOrEmpty(WEB_CLIENT_PENDING_REQUEST_TIME)
                                                        .map(o -> (ContextWithStartTime) o)
                                                        .orElse(null);

                                                if (contextWithStartTime == null) return;

                                                Duration duration = Duration.ofNanos(System.nanoTime() - contextWithStartTime.startTime());
                                                DummyLogger.log(this, "Http client pending request took: " + duration);
                                                meterRegistry
                                                        .timer("custom.web.client.queue_wait_time")
                                                        .record(duration);
                                            })
                                    )
                                    .build()
                    )
                    .build();
        }

        private ReactorResourceFactory createReactorResourceFactory() {
            ReactorResourceFactory factory = new ReactorResourceFactory();
            factory.setUseGlobalResources(false);
            factory.setLoopResources(LoopResources.create("custom-http-client"));
            return factory;
        }
    }
}