package com.nalepa.demo.common.monitored

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import org.springframework.context.SmartLifecycle
import org.springframework.scheduling.concurrent.CustomizableThreadFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Collections
import java.util.concurrent.*


@Component
class ExecutorsShutdownManager: SmartLifecycle {

    private val createdExecutors =
        Collections.synchronizedList(mutableListOf<ExecutorService>())

    fun addExecutorService(executorService: ExecutorService) {
        createdExecutors.add(executorService)
    }


    private var running = true

    override fun start() {
    }

    override fun stop() {
        shutdownExecutors()
        running = false
    }

    override fun isRunning(): Boolean = running

    fun shutdownExecutors() {
        // 1. signal to end executors to end
        createdExecutors.forEach { it.shutdown() }

        // 2. parallel wait
        val futures = createdExecutors.map { executor ->
            CompletableFuture.supplyAsync {
                executor.awaitTermination(5, TimeUnit.SECONDS) to executor
            }
        }

        // 3. check if executors are finished or force shutdown
        futures.forEach { future ->
            val (isTerminated, executor) = future.get()
            if (!isTerminated) {
                executor.shutdownNow()
            }
        }
    }

}
