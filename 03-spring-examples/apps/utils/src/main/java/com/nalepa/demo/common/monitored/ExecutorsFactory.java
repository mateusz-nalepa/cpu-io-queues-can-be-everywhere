package com.nalepa.demo.common.monitored;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.*;

@Component
public class ExecutorsFactory {

    private final MeterRegistry meterRegistry;
    private final ExecutorsShutdownManager executorsShutdownManager;

    public ExecutorsFactory(MeterRegistry meterRegistry, ExecutorsShutdownManager executorsShutdownManager) {
        this.meterRegistry = meterRegistry;
        this.executorsShutdownManager = executorsShutdownManager;
    }

    public Executor monitorExecutorForVirtualThreads(
            Executor delegate,
            String logMessagePrefix,
            String threadPoolName
    ) {
        return ExecutorServiceMetrics.monitor(meterRegistry, delegate, threadPoolName);
    }

    public ExecutorService create(
            String logMessagePrefix,
            String threadPoolName,
            int threadsSize,
            int taskQueueSize
    ) {
        ThreadPoolExecutor executor = createThreadPoolExecutor(threadPoolName, threadsSize, taskQueueSize);

        executorsShutdownManager.addExecutorService(executor);

        return monitorExecutorService(executor, logMessagePrefix, threadPoolName);
    }

    public ExecutorService monitorExecutorService(
            ExecutorService delegate,
            String logMessagePrefix,
            String threadPoolName
    ) {
        return ExecutorServiceMetrics.monitor(meterRegistry, delegate, threadPoolName);
    }

    private ThreadPoolExecutor createThreadPoolExecutor(
            String threadPrefix,
            int threads,
            int taskQueueSize
    ) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                // by default, Java minimize resources used when dealing with threads, so threads are created when there is reached queue limit
                // so yolo, core pool size is equal here always to max pool size
                // but also, we will reach core when needed, cause there is no `executor.prestartAllCoreThreads()` used
                threads,
                threads,
                Duration.ofSeconds(60).toMillis(), // ignored, cause core == max
                TimeUnit.MILLISECONDS,             // ignored, cause core == max
                new LinkedBlockingQueue<>(taskQueueSize),
                new CustomizableThreadFactory(threadPrefix + "-"),
                new ThreadPoolExecutor.AbortPolicy()
        );

        executor.prestartAllCoreThreads();

        return executor;
    }
}