package com.nalepa.demo.controller

import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.httpclient.HttpDataProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@RestController
class CoroutinesClassicWebControllerDefaults(
    private val httpDataProvider: HttpDataProvider,
) {

    @GetMapping("/endpoint/scenario/defaults/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    suspend fun dummyEndpoint(
        @PathVariable index: String,
        @PathVariable mockDelaySeconds: Long,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start endpoint for index: $index")

        return httpDataProvider.getData(index, mockDelaySeconds)
            .also { Operations.heavyCpuCode(cpuOperationDelaySeconds) }
            .let { ResponseEntity.ok(it) }
    }

}
