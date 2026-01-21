package com.nalepa.demo.httpclient

import com.nalepa.demo.common.DUMMY_INDEX
import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.SomeResponse
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class HttpDataProvider(
    private val httpClientFactory: HttpClientFactory,
) {

    private val webClient = httpClientFactory.createWebClient()

    fun getData(index: String, mockDelaySeconds: Long): Mono<SomeResponse> {
        val startTime = System.nanoTime()

        return webClient
            .get()
            .uri("http://localhost:8082/mock/{index}/{mockDelaySeconds}", index, mockDelaySeconds )
            .header(DUMMY_INDEX, index)
            .retrieve()
            // by default webClient thread will do deserialization, switch it to another thread pool if needed with publishOn
            .bodyToMono(SomeResponse::class.java)
            .doOnNext {
                val duration = Duration.ofNanos(System.nanoTime() - startTime)
                DummyLogger.log(this, "Index: $index. Got response from webClient after: $duration")
            }
    }

}