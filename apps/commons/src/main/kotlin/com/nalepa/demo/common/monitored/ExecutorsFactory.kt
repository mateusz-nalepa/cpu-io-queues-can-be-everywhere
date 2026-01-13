package com.nalepa.demo.common.monitored

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.SmartLifecycle
import org.springframework.scheduling.concurrent.CustomizableThreadFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.*


@Component
class ExecutorsFactory(
    private val meterRegistry: MeterRegistry,
) : SmartLifecycle {

    fun monitorExecutor(
        delegate: Executor,
        logMessagePrefix: String,
        metricName: String,
    ): Executor {
        return MonitoredExecutor(
            delegate,
            logMessagePrefix,
            metricName,
            meterRegistry,
        )
    }

    fun monitorExecutorService(
        delegate: ExecutorService,
        logMessagePrefix: String,
        threadPoolName: String,
    ): ExecutorService {
        return MonitoredExecutorService(
            delegate,
            logMessagePrefix,
            threadPoolName,
            meterRegistry,
        )
    }

    fun create(
        logMessagePrefix: String,
        threadPoolName: String,
        threadsSize: Int,
        taskQueueSize: Int,
    ): ExecutorService {
        return MonitoredExecutorService(
            createThreadPoolExecutor(
                threadPoolName,
                threadsSize,
                taskQueueSize,
            ),
            logMessagePrefix,
            threadPoolName,
            meterRegistry,
        )
    }

    private fun createThreadPoolExecutor(
        threadPrefix: String,
        threads: Int,
        taskQueueSize: Int,
    ): ThreadPoolExecutor {
        val executor =
            ThreadPoolExecutor(
                // by default, Java minimize resources used when dealing with threads, so threads are created when there is reached queue limit
                // so yolo, core pool size is equal here always to max pool size
                // but also, we will reach core when needed, cause there is no `executor.prestartAllCoreThreads()` used
                threads,
                threads,
                Duration.ofSeconds(60).toMillis(), // ignored, cause core == max
                TimeUnit.MILLISECONDS, // ignored, cause core == max
                LinkedBlockingQueue(taskQueueSize),
                CustomizableThreadFactory("$threadPrefix-"),
                ThreadPoolExecutor.AbortPolicy(),
            )

        createdExecutors.add(executor)

        return executor
    }

    private val createdExecutors = mutableListOf<ThreadPoolExecutor>()
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
