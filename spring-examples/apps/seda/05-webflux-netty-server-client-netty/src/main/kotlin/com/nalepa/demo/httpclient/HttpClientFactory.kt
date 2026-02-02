package com.nalepa.demo.httpclient

import com.nalepa.demo.common.DUMMY_INDEX
import com.nalepa.demo.common.DummyLogger
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder
import org.springframework.http.client.ReactorResourceFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.resources.LoopResources
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

const val WEB_CLIENT_PENDING_REQUEST_TIME = "WEB_CLIENT_PENDING_REQUEST_TIME"

data class ContextWithStartTime(
    val index: String,
    val startTime: Long,
)

@Component
class HttpClientFactory(
    private val webClientBuilder: WebClient.Builder,
    private val reactorClientHttpConnectorBuilder: ReactorClientHttpConnectorBuilder,
    private val meterRegistry: MeterRegistry,
) {

    fun createWebClient(): WebClient {
        return webClientBuilder
            // filter and connector workaround
            .filter { request, next ->
                next.exchange(request)
                    .contextWrite {
                        // executed on server thread
                        val index = request.headers()[DUMMY_INDEX]?.first()
                        if (index != null) {
                            DummyLogger.log(
                                this,
                                "WORKAROUND: Index: $index. Start $WEB_CLIENT_PENDING_REQUEST_TIME"
                            )
                            it.put(WEB_CLIENT_PENDING_REQUEST_TIME, ContextWithStartTime(index, System.nanoTime()))
                        } else {
                            it
                        }
                    }
            }
            .clientConnector(
                reactorClientHttpConnectorBuilder
                    // dedicated thread pool for WebClient
                    .withReactorResourceFactory(
                        ReactorResourceFactory().apply {
                            this.isUseGlobalResources = false
                            this.loopResources = LoopResources.create("custom-http-client")
                        }
                    )
                    .withHttpClientCustomizer { httpClient ->
                        httpClient
                            .doOnRequest { httpClientRequest, _ ->
                                // executed on webClient thread
                                val contextWithStartTime =
                                    httpClientRequest
                                        .currentContextView()
                                        .getOrEmpty<ContextWithStartTime>(WEB_CLIENT_PENDING_REQUEST_TIME)
                                        .getOrNull()
                                        ?: return@doOnRequest

                                val duration = Duration.ofNanos(System.nanoTime() - contextWithStartTime.startTime)
                                DummyLogger.log(this, "Http client pending request took: $duration")
                                meterRegistry
                                    .timer("custom.web.client.saturation")
                                    .record(duration)
                            }
                    }
                    .build()
            )
            .build()
    }

}