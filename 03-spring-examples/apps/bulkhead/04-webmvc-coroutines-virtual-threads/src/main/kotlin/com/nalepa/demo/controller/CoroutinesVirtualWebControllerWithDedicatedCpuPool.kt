package com.nalepa.demo.controller

import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.common.monitored.ExecutorsFactory
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@RestController
class CoroutinesVirtualWebControllerWithDedicatedCpuPool(
    private val executorsFactory: ExecutorsFactory,
) {

    // thanks to this executor, server threads are like I/O threads
    // executor for fast endpoint is not really needed here,
    // but I want to show that we can have different executors for different endpoints
    private val dispatcherForSlow =
        executorsFactory.create(
            ExecutorsFactory.ThreadPoolConfig.builder()
                .threadPoolName("pool.for.slow")
                .threadsSize(Runtime.getRuntime().availableProcessors())
                .taskQueueSize(Runtime.getRuntime().availableProcessors())
                .build()
        ).asCoroutineDispatcher()

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/fast/{index}")
    suspend fun fastEndpointDedicatedCpuPool(
        @PathVariable index: String,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start FAST endpoint for index: $index")

        return ResponseEntity.ok((SomeResponse("fast")))
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/slow/{index}/{cpuAppOrSleep}")
    suspend fun slowEndpointDedicatedCpuPool(
        @PathVariable index: String,
        @PathVariable cpuAppOrSleep: Long,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start SLOW endpoint for index: $index")

        return withContext(dispatcherForSlow) {
            // NOTE: in real app remember for example about: deserialize requestBody, remember about validation
            Operations.heavyCpuCode(cpuAppOrSleep)
            ResponseEntity.ok((SomeResponse("fast")))
        }
    }

}
