package com.nalepa.demo.controller

import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.common.monitored.ExecutorsFactory
import com.nalepa.demo.httpclient.HttpDataProvider
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@RestController
class CoroutinesVirtualWebControllerDefaultsWithDedicatedCpuPool(
    private val httpDataProvider: HttpDataProvider,
    private val executorsFactory: ExecutorsFactory,
) {

    private val afterRestClientDispatcher =
        executorsFactory.create(
            "CPU Bound pool waiting time took:",
            "CPU.after.restClient",
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
        return httpDataProvider // http is being executed on a virtual thread pool :3
            .getData(index, mockDelaySeconds)
            .also { executeHeavyCpuOperation(cpuOperationDelaySeconds) }
            .let { ResponseEntity.ok(it) }
    }

    private suspend fun executeHeavyCpuOperation(cpuOperationDelaySeconds: Long) {
        withContext(afterRestClientDispatcher) {
            Operations.heavyCpuCode(cpuOperationDelaySeconds)
        }

    }

}
