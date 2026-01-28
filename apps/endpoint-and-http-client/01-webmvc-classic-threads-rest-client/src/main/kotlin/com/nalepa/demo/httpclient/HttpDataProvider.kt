package com.nalepa.demo.httpclient

import com.nalepa.demo.common.DUMMY_INDEX
import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.SomeResponse
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class HttpDataProvider(
    private val httpClientFactory: HttpClientFactory,
) {

    private val restClient = httpClientFactory.createRestClient()

    fun getData(index: String, mockDelaySeconds: Long): SomeResponse {
        val startTime = System.nanoTime()

        return restClient
            .get()
            .uri("http://localhost:8082/mock/{index}/{mockDelaySeconds}", index, mockDelaySeconds)
            .header(DUMMY_INDEX, index)
            .retrieve()
            .body(SomeResponse::class.java)!!
            .also {
                val duration = Duration.ofNanos(System.nanoTime() - startTime)
                DummyLogger.log(this, "Index: $index. Got response from restClient after: $duration")
            }
    }
}