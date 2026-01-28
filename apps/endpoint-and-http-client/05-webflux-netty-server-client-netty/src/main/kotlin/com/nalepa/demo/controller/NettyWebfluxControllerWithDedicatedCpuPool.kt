package com.nalepa.demo.controller

import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.common.monitored.ExecutorsFactory
import com.nalepa.demo.httpclient.HttpDataProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
class NettyWebfluxControllerWithDedicatedCpuPool(
    private val httpDataProvider: HttpDataProvider,
    private val executorsFactory: ExecutorsFactory,
) {

    // I don't know how to replace reactor pool with custom pool, so basically thanks to this pool, reactor server threads are like I/O threads
    private val serverWorkerScheduler =
        Schedulers.fromExecutor(
            executorsFactory.create(
                "CPU Bound pool waiting time took:",
                "CPU.for.serverWorker",
                threadsSize = Runtime.getRuntime().availableProcessors(),
                taskQueueSize = Runtime.getRuntime().availableProcessors(),
            )
        )

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
            .publishOn(serverWorkerScheduler)
            // .map {  } // eq: deserialize requestBody, remember about validation if needed
            .flatMap {
                httpDataProvider
                    .getData(index, mockDelaySeconds)
            }
            .publishOn(afterWebClientScheduler)
            .doOnNext { Operations.heavyCpuCode(cpuOperationDelaySeconds) }
            .map { ResponseEntity.ok(it) }
    }

}