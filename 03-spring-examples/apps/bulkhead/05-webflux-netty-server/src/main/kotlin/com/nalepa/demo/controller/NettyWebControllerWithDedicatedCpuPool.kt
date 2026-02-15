package com.nalepa.demo.controller

import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.common.monitored.ExecutorsFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers


@RestController
class NettyWebControllerWithDedicatedCpuPool(
    private val executorsFactory: ExecutorsFactory,
) {

    // thanks to those executors, server threads are like I/O threads
    private val schedulerForFast =
        Schedulers.fromExecutor(
            executorsFactory.create(
                "CPU Bound pool waiting time took:",
                "CPU.for.fast",
                threadsSize = Runtime.getRuntime().availableProcessors(),
                taskQueueSize = Runtime.getRuntime().availableProcessors(),
            )
        )

    // in theory this is not needed, in practice in reactor I have no idea how to monitor tasks queue wait time for server
    private val schedulerForSlow =
        Schedulers.fromExecutor(
            executorsFactory.create(
                "CPU Bound pool waiting time took:",
                "CPU.for.slow",
                threadsSize = Runtime.getRuntime().availableProcessors(),
                taskQueueSize = Runtime.getRuntime().availableProcessors(),
            )
        )

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/fast/{index}")
    fun fastEndpointDedicatedCpuPool(
        @PathVariable index: String,
    ): Mono<ResponseEntity<SomeResponse>> {
        DummyLogger.log(this, "Start FAST endpoint for index: $index")

        return Mono
            .just(index)
            .publishOn(schedulerForFast)
            .map {
                // some processing here
                ResponseEntity.ok((SomeResponse("fast")))
            }
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/slow/{index}/{cpuOperationDelaySeconds}")
    fun slowEndpointDedicatedCpuPool(
        @PathVariable index: String,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): Mono<ResponseEntity<SomeResponse>> {

        DummyLogger.log(this, "Start SLOW endpoint for index: $index")

        return Mono
            .just(index)
            .publishOn(schedulerForSlow)
            .doOnNext { Operations.heavyCpuCode(cpuOperationDelaySeconds) }
            .map {
                // some processing here
                ResponseEntity.ok((SomeResponse("slow")))
            }
    }


}
