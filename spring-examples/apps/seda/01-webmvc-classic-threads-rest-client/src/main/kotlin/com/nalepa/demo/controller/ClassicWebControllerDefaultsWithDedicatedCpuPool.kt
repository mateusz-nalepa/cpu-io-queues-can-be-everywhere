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
    private val httpDataProvider: HttpDataProvider,
    private val executorsFactory: ExecutorsFactory,
) {


    // IO-bound: hide latency, avoid queue wait time.
    // Context switching here is helpful — CPU usage for IO is basically 0%
    private val ioExecutor =
        executorsFactory.create(
            "IO Bound Pool",
            "io",
            threadsSize = 500,
            taskQueueSize = 10,
        )

    // CPU-bound: small queue is fine; high CPU usage should trigger scaling.
    // Context switching here is expensive — CPU is the bottleneck
    private val cpuExecutor =
        executorsFactory.create(
            "CPU Bound Pool",
            "cpu",
            threadsSize = Runtime.getRuntime().availableProcessors(),
            taskQueueSize = 100,
        )

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    fun dummyEndpoint(
        @PathVariable index: String,
        @PathVariable mockDelaySeconds: Long,
        @PathVariable cpuOperationDelaySeconds: Long,
    ): CompletableFuture<ResponseEntity<SomeResponse>> {
        DummyLogger.log(this, "Start endpoint for index: $index")

        return startAsyncOn(ioExecutor)
            .with { httpDataProvider.getData(index, mockDelaySeconds) }
            .publishOn(cpuExecutor)
            .map {
                Operations.heavyCpuCode(cpuOperationDelaySeconds)
                it
            }
            .map { ResponseEntity.ok(it) }
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