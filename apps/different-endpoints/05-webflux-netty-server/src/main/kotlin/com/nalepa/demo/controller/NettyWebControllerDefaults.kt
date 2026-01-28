package com.nalepa.demo.controller

import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


@RestController
class NettyWebControllerDefaults {

    @GetMapping("/endpoint/scenario/defaults/fast/{index}")
    fun fastEndpointDefaults(
        @PathVariable index: String,
    ): Mono<ResponseEntity<SomeResponse>> {
        DummyLogger.log(this, "Start FAST endpoint for index: $index")

        return Mono.just(ResponseEntity.ok((SomeResponse("fast"))))
    }

    @GetMapping("/endpoint/scenario/defaults/slow/{index}/{cpuOperationDelaySeconds}")
    fun slowEndpointDefaults(
        @PathVariable index: String,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): Mono<ResponseEntity<SomeResponse>> {
        DummyLogger.log(this, "Start SLOW endpoint for index: $index")

        Operations.heavyCpuCode(cpuOperationDelaySeconds)

        return Mono.just(ResponseEntity.ok((SomeResponse("fast"))))
    }

}
