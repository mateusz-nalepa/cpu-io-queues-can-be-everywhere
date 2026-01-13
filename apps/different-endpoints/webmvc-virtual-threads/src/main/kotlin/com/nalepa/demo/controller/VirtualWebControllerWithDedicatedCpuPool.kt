package com.nalepa.demo.controller

import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.common.monitored.ExecutorsFactory
import com.nalepa.demo.common.safeGetOnVirtual
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@RestController
class VirtualWebControllerWithDedicatedCpuPool(
    private val executorsFactory: ExecutorsFactory,
) {

    private val executor =
        executorsFactory.create(
            "CPU Bound pool waiting time took:",
            "CPU.for.slow",
            threadsSize = Runtime.getRuntime().availableProcessors(),
            taskQueueSize = Runtime.getRuntime().availableProcessors(),
        )

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/fast/{index}")
    fun fastEndpointDedicatedCpuPool(
        @PathVariable index: String,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start FAST endpoint for index: $index")

        return ResponseEntity.ok((SomeResponse("fast")))
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/slow/{index}/{cpuOperationDelaySeconds}")
    fun slowEndpointDedicatedCpuPool(
        @PathVariable index: String,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start SLOW endpoint for index: $index")

        executeHeavyCpuOperation(cpuOperationDelaySeconds)

        return ResponseEntity.ok((SomeResponse("fast")))
    }

    private fun executeHeavyCpuOperation(cpuOperationDelaySeconds: Long) {
        executor
            .submit { Operations.heavyCpuCode(cpuOperationDelaySeconds) }
            .safeGetOnVirtual()
    }

}
