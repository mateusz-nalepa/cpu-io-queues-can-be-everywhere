package com.nalepa.demo.controller

import com.nalepa.demo.common.DUMMY_INDEX
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


@RestController
class CoroutinesVirtualWebControllerDefaultsWithDedicatedCpuPool(
    private val httpClientFactory: HttpClientFactory,
    private val executorsFactory: ExecutorsFactory,
    private val objectMapper: ObjectMapper,
) {
    private val restClient = httpClientFactory.createRestClient()

    private val cpuDispatcher =
        executorsFactory.create(
            "CPU Bound pool waiting time took:",
            "CPU.dispatcher",
            threadsSize = Runtime.getRuntime().availableProcessors(),
            taskQueueSize = Runtime.getRuntime().availableProcessors(),
        ).asCoroutineDispatcher()

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    suspend fun dummyEndpoint(
        @PathVariable index: String,
        @PathVariable mockDelaySeconds: Long,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start endpoint for index: $index")

        // NOTE: if needed, deserialize request on another cpu pool, remember about validation if present
        // http is being executed on a virtual thread pool :3
        return getData(index, mockDelaySeconds)
            .let { byteArray ->
                executeHeavyCpuOperation(byteArray, cpuOperationDelaySeconds)
            }
            .let { ResponseEntity.ok(it) }
    }

    private suspend fun executeHeavyCpuOperation(
        byteArray: ByteArray,
        cpuOperationDelaySeconds: Long
    ): SomeResponse =
        withContext(cpuDispatcher) {
            val someResponse = objectMapper.readValue(byteArray, SomeResponse::class.java)
            Operations.heavyCpuCode(cpuOperationDelaySeconds)
            someResponse
        }

    private fun getData(index: String, mockDelaySeconds: Long): ByteArray {
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
