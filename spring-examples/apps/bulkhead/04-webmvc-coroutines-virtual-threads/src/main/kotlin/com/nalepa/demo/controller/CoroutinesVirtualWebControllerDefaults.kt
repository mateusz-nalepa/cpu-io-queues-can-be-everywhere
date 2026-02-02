package com.nalepa.demo.controller

import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@RestController
class CoroutinesVirtualWebControllerDefaults {

    @GetMapping("/endpoint/scenario/defaults/fast/{index}")
    suspend fun fastEndpointDefaults(
        @PathVariable index: String,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start FAST endpoint for index: $index")

        return ResponseEntity.ok((SomeResponse("fast")))
    }

    @GetMapping("/endpoint/scenario/defaults/slow/{index}/{cpuAppOrSleep}")
    suspend fun slowEndpointDefaults(
        @PathVariable index: String,
        @PathVariable cpuAppOrSleep: Long,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start SLOW endpoint for index: $index")

        Operations.someBlockingIO(cpuAppOrSleep)

        return ResponseEntity.ok((SomeResponse("fast")))
    }

}
