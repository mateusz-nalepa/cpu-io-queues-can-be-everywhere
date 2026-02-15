package com.nalepa.demo.controller

import com.nalepa.demo.common.DUMMY_INDEX
import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.common.monitored.ExecutorsFactory
import com.nalepa.demo.httpclient.HttpClientFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@RestController
class NettyWebfluxControllerWithDedicatedCpuPool(
    private val executorsFactory: ExecutorsFactory,
    private val httpClientFactory: HttpClientFactory,
    private val objectMapper: ObjectMapper,
) {

    private val webClient = httpClientFactory.createWebClient()

    private val afterWebClientScheduler =
        Schedulers.fromExecutor(
            executorsFactory.create(
                "CPU Bound pool waiting time took:",
                "CPU.for.afterWebClient",
                threadsSize = Runtime.getRuntime().availableProcessors(),
                taskQueueSize = Runtime.getRuntime().availableProcessors(),
            )
        )

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    fun dummyEndpoint(
        @PathVariable index: String,
        @PathVariable mockDelaySeconds: Long,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): Mono<ResponseEntity<SomeResponse>> {

        DummyLogger.log(this, "Start endpoint for index: $index")

        return Mono.just(index)
            // .map {  } // eq: deserialize requestBody, remember about validation if needed
            .flatMap {
                getData(index, mockDelaySeconds)
            }
            .publishOn(afterWebClientScheduler)
            .map { byteArray ->
                val someResponse = objectMapper.readValue(byteArray, SomeResponse::class.java)
                Operations.heavyCpuCode(cpuOperationDelaySeconds)
                someResponse
            }
            .map { ResponseEntity.ok(it) }
    }

    private fun getData(index: String, mockDelaySeconds: Long): Mono<ByteArray> {
        val startTime = System.nanoTime()

        return webClient
            .get()
            .uri("http://localhost:8082/mock/{index}/{mockDelaySeconds}", index, mockDelaySeconds)
            .header(DUMMY_INDEX, index)
            .retrieve()
            // by default webClient thread will do deserialization, switch it to another thread pool if needed with publishOn
            .bodyToMono(ByteArray::class.java)
            .doOnNext {
                val duration = Duration.ofNanos(System.nanoTime() - startTime)
                DummyLogger.log(this, "Index: $index. Got response from webClient after: $duration")
            }
    }


}