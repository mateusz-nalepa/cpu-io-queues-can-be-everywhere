package com.nalepa.demo.controller

import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.common.async
import com.nalepa.demo.common.monitored.ExecutorsFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture


@RestController
class ClassicWebControllerWithDedicatedCpuPool(
    private val executorsFactory: ExecutorsFactory,
) {

    // thanks to those executors, tomcat threads are like I/O threads

    private val executorForFast =
        executorsFactory.create(
            "CPU Bound pool for fast waiting time took:",
            "CPU.for.fast",
            threadsSize = 200,
            taskQueueSize = 200,
        )

    private val executorForSlow =
        executorsFactory.create(
            "CPU Bound pool for slow waiting time took:",
            "CPU.for.slow",
            threadsSize = 200,
            taskQueueSize = 200,
        )

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/fast/{index}")
    fun fastEndpointDedicatedCpuPool(
        @PathVariable index: String,
    ): CompletableFuture<ResponseEntity<SomeResponse>> {
        DummyLogger.log(this, "Start FAST endpoint for index: $index")

        return async(on = executorForFast) {
            ResponseEntity.ok((SomeResponse("slow")))
        }
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/slow/{index}/{cpuAppOrSleep}")
    fun slowEndpointDedicatedCpuPool(
        @PathVariable index: String,
        @PathVariable cpuAppOrSleep: Long,
    ): CompletableFuture<ResponseEntity<SomeResponse>> {
        DummyLogger.log(this, "Start SLOW endpoint for index: $index")

        return async(on = executorForSlow) {
            // NOTE: in real app remember for example about: deserialize requestBody, remember about validation
            Operations.someBlockingIO(cpuAppOrSleep)
            ResponseEntity.ok((SomeResponse("fast")))
        }
    }

}
