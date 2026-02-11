package com.nalepa.demo.controller

import com.nalepa.demo.common.DUMMY_INDEX
import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.httpclient.HttpClientFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper
import java.time.Duration


@RestController
class CoroutinesClassicWebControllerDefaults(
    private val httpClientFactory: HttpClientFactory,
    private val objectMapper: ObjectMapper
) {

    private val restClient = httpClientFactory.createRestClient()

    @GetMapping("/endpoint/scenario/defaults/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    suspend fun dummyEndpoint(
        @PathVariable index: String,
        @PathVariable mockDelaySeconds: Long,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start endpoint for index: $index")

        return getData(index, mockDelaySeconds)
            .let { byteArray ->
                val someResponse = objectMapper.readValue(byteArray, SomeResponse::class.java)
                Operations.heavyCpuCode(cpuOperationDelaySeconds)
                someResponse
            }
            .let { ResponseEntity.ok(it) }
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
