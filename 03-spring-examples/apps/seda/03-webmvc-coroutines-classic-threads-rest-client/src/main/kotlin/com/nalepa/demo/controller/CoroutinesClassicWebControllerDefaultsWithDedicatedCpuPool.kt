package com.nalepa.demo.controller

import com.nalepa.demo.common.Constants.DUMMY_INDEX
import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.common.monitored.ExecutorsFactory
import com.nalepa.demo.httpclient.HttpClientFactory
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper
import java.time.Duration

/**
 * request
 *    -> IO Pool
 *    -> CPU Pool
 *    -> another IO Pool (if needed)
 *    -> another CPU Pool (if needed)
 */
@RestController
class ClassicWebControllerDefaultsWithDedicatedCpuPool(
    private val httpClientFactory: HttpClientFactory,
    private val executorsFactory: ExecutorsFactory,
    private val objectMapper: ObjectMapper,
) {

    private val restClient = httpClientFactory.createRestClient()

    // IO-bound: hide latency, avoid queue wait time.
    // Context switching here is helpful — CPU usage for IO is basically 0%
    private val ioDispatcher =
        executorsFactory.create(
            ExecutorsFactory.ThreadPoolConfig.builder()
                .threadPoolName("io.pool")
                .threadsSize(400) // in order to prevent microburst present in demonstration :D
                // in normal situation, this queue should not be so high, there should be more threads
                // app should wait for response from I/O, not sit down in queue here
                .taskQueueSize(400)
                .build()
        ).asCoroutineDispatcher()

    private val cpuDispatcher =
        executorsFactory.create(
            ExecutorsFactory.ThreadPoolConfig.builder()
                .threadPoolName("cpu.pool")
                // thread size should be equal to cpu Runtime.getRuntime().availableProcessors()
                // here is 200 for demonstration purposes
                // cause tomcat default is 200
                // so it's easier to demonstrate xd
                .threadsSize(200)
                // queue size can be high,
                // because when CPU is bottleneck, it's better to wait in queue
                // than process all requests at the same time
                .taskQueueSize(400)
                .build()
        ).asCoroutineDispatcher()

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    suspend fun dummyEndpoint(
        @PathVariable index: String,
        @PathVariable mockDelaySeconds: Long,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start endpoint for index: $index")

        val byteArray =
            withContext(ioDispatcher) {
                getData(index, mockDelaySeconds)
            }
        val someResponse =
            withContext(cpuDispatcher) {
                val someResponse = objectMapper.readValue(byteArray, SomeResponse::class.java)
                Operations.heavyCpuCode(cpuOperationDelaySeconds)
                someResponse
            }
        return ResponseEntity.ok(someResponse)
    }

    fun getData(index: String, mockDelaySeconds: Long): ByteArray {
        val startTime = System.nanoTime()

        return restClient
            .get()
            .uri("http://localhost:8082/mock/{index}/{mockDelaySeconds}", index, mockDelaySeconds)
            .header(DUMMY_INDEX, index)
            .retrieve()
            .body(ByteArray::class.java)!!
            .also {
                val duration = Duration.ofNanos(System.nanoTime() - startTime)
                DummyLogger.log(this, "Index: $index. Got response from restClient after: $duration")
            }
    }


}
