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

/**
 * request
 *    -> IO Pool
 *    -> CPU Pool
 *    -> another IO Pool (if needed)
 *    -> another CPU Pool (if needed)
 */
@RestController
class ClassicWebControllerDefaultsWithDedicatedCpuPool(
    private val httpDataProvider: HttpDataProvider,
    private val executorsFactory: ExecutorsFactory,
) {


    // IO-bound: hide latency, avoid queue wait time.
    // Context switching here is helpful — CPU usage for IO is basically 0%
    private val ioDispatcher =
        executorsFactory.create(
            "IO Bound Pool",
            "io",
            threadsSize = 500,
            taskQueueSize = 10,
        ).asCoroutineDispatcher()

    // CPU-bound: small queue is fine; high CPU usage should trigger scaling.
    // Context switching here is expensive — CPU is the bottleneck
    private val cpuDispatcher =
        executorsFactory.create(
            "CPU Bound Pool",
            "cpu",
            threadsSize = Runtime.getRuntime().availableProcessors(),
            taskQueueSize = 100,
        ).asCoroutineDispatcher()

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    suspend fun dummyEndpoint(
        @PathVariable index: String,
        @PathVariable mockDelaySeconds: Long,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): ResponseEntity<SomeResponse> {
        DummyLogger.log(this, "Start endpoint for index: $index")

        val response = withContext(ioDispatcher) {
            httpDataProvider.getData(index, mockDelaySeconds)
        }
        withContext(cpuDispatcher) {
            Operations.heavyCpuCode(cpuOperationDelaySeconds)
            response
        }
        return ResponseEntity.ok(response)
    }

}
