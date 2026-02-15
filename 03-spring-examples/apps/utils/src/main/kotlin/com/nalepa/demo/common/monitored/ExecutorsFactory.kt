package com.nalepa.demo.common.monitored

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import org.springframework.scheduling.concurrent.CustomizableThreadFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.*


@Component
class ExecutorsFactory(
    private val meterRegistry: MeterRegistry,
    private val executorsShutdownManager: ExecutorsShutdownManager,
) {

    fun monitorExecutorForVirtualThreads(
        delegate: Executor,
        logMessagePrefix: String,
        threadPoolName: String,
    ): Executor {
        return ExecutorServiceMetrics
            .monitor(
                meterRegistry,
                delegate,
                threadPoolName,
            )
    }


    fun create(
        logMessagePrefix: String,
        threadPoolName: String,
        threadsSize: Int,
        taskQueueSize: Int,
    ): ExecutorService {
        val executor =
            createThreadPoolExecutor(
                threadPoolName,
                threadsSize,
                taskQueueSize,
            )

        executorsShutdownManager.addExecutorService(executor)

        return monitorExecutorService(executor, logMessagePrefix, threadPoolName)
    }

    fun monitorExecutorService(
        delegate: ExecutorService,
        logMessagePrefix: String,
        threadPoolName: String,
    ): ExecutorService {
        return ExecutorServiceMetrics
            .monitor(
                meterRegistry,
                delegate,
                threadPoolName,
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

        executor.prestartAllCoreThreads()

        return executor
    }

}

