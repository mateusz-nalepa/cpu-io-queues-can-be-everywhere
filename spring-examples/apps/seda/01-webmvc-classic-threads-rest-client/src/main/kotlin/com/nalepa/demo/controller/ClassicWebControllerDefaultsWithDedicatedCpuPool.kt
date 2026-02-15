package com.nalepa.demo.controller

import com.nalepa.demo.common.DUMMY_INDEX
import com.nalepa.demo.common.DummyLogger
import com.nalepa.demo.common.Operations
import com.nalepa.demo.common.SomeResponse
import com.nalepa.demo.common.monitored.ExecutorsFactory
import com.nalepa.demo.httpclient.HttpClientFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Function

/**
 * request
 *    -> IO Pool
 *    -> CPU Pool
 *    -> another IO Pool (if needed)
 *    -> another CPU Pool (if needed)
 */
@RestController
class ClassicWebControllerDefaultsWithDedicatedCpuPool(
    private val httpClientFactory: HttpClientFactory,
    private val executorsFactory: ExecutorsFactory,
    private val objectMapper: ObjectMapper,
) {

    private val restClient = httpClientFactory.createRestClient()

    // IO-bound: hide latency, avoid queue wait time.
    // Context switching here is helpful — CPU usage for IO is basically 0%
    private val ioExecutor =
        executorsFactory.create(
            "IO Bound Pool",
            "io",
            threadsSize = 400,
            // in order to prevent microburst present in demonstration :D
            // in normal situation, this queue should not be so high, there should be more threads
            // app should wait for response from I/O, not sit down in queue here
            taskQueueSize = 400,
        )

    // CPU-bound: small queue is fine; high CPU usage should trigger scaling.
    // Context switching here is expensive — CPU is the bottleneck
    private val cpuExecutor =
        executorsFactory.create(
            "CPU Bound Pool",
            "cpu",
            // In normal conditions, set Runtime.getRuntime().availableProcessors()
            // 200 is only for demonstration purposes in this repo
            threadsSize = 200,
            taskQueueSize = 400,
        )

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    fun dummyEndpoint(
        @PathVariable index: String,
        @PathVariable mockDelaySeconds: Long,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): CompletableFuture<ResponseEntity<SomeResponse>> {
        DummyLogger.log(this, "Start endpoint for index: $index")

        return startAsyncOn(ioExecutor)
            .with { getData(index, mockDelaySeconds) }
            .publishOn(cpuExecutor)
            .map { byteArray ->
                val someResponse = objectMapper.readValue(byteArray, SomeResponse::class.java)
                Operations.heavyCpuCode(cpuOperationDelaySeconds)
                someResponse
            }
            .map { ResponseEntity.ok(it) }
    }

    private fun getData(index: String, mockDelaySeconds: Long): ByteArray {
        val startTime = System.nanoTime()

        return restClient
            .get()
            .uri("http://localhost:8082/mock/{index}/{mockDelaySeconds}", index, mockDelaySeconds)
            .header(DUMMY_INDEX, index)
            .retrieve()
            .body(ByteArray::class.java)!!
            .also {
                val duration = Duration.ofNanos(System.nanoTime() - startTime)
                DummyLogger.log(this, "Index: $index. Got response from restClient after: $duration")
            }
    }


}


// in order to have something like: SLR - Single Line Responsibility
fun startAsyncOn(executor: Executor): CompletableFuture<Unit> {
    return CompletableFuture.supplyAsync({ Unit }, executor)
}

fun <T> CompletableFuture<T>.publishOn(executor: Executor): CompletableFuture<T> {
    return this.thenApplyAsync(Function.identity(), executor)
}

fun <U> CompletableFuture<Unit>.with(action: Function<Unit, U>): CompletableFuture<U> {
    return this.thenApply(action)
}

fun <T, U> CompletableFuture<T>.map(action: Function<T, U>): CompletableFuture<U> {
    return this.thenApply(action)
}

fun <T, U> CompletableFuture<T>.flatMap(action: Function<T, CompletableFuture<U>>): CompletableFuture<U> {
    return this.thenCompose(action)
}