package com.nalepa.demo.common.monitored;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class ExecutorsFactory {

    private final MeterRegistry meterRegistry;
    private final ExecutorsShutdownManager executorsShutdownManager;

    public ExecutorsFactory(MeterRegistry meterRegistry, ExecutorsShutdownManager executorsShutdownManager) {
        this.meterRegistry = meterRegistry;
        this.executorsShutdownManager = executorsShutdownManager;
    }

    public ExecutorService create(ThreadPoolConfig threadPoolConfig) {
        ThreadPoolExecutor executor = createThreadPoolExecutor(threadPoolConfig);

        executorsShutdownManager.addExecutorService(executor);

        return monitorExecutorService(executor, threadPoolConfig.threadPoolName);
    }


    public ExecutorService monitorExecutorService(
            ExecutorService delegate,
            String threadPoolName
    ) {
        return ExecutorServiceMetrics.monitor(meterRegistry, delegate, threadPoolName);
    }

    private ThreadPoolExecutor createThreadPoolExecutor(
            ThreadPoolConfig threadPoolConfig
    ) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                // by default, Java minimize resources used when dealing with threads, so threads are created when there is reached queue limit
                // so yolo, core pool size is equal here always to max pool size
                // but also, we will reach core when needed, cause there is no `executor.prestartAllCoreThreads()` used
                threadPoolConfig.threadsSize,
                threadPoolConfig.threadsSize,
                Duration.ofSeconds(60).toMillis(), // ignored, cause core == max
                TimeUnit.MILLISECONDS,             // ignored, cause core == max
                new LinkedBlockingQueue<>(threadPoolConfig.taskQueueSize),
                new CustomizableThreadFactory(threadPoolConfig.threadPoolName + "-"),
                new ThreadPoolExecutor.AbortPolicy()
        );

        executor.prestartAllCoreThreads();

        return executor;
    }

    public static class ThreadPoolConfig {

        private final String threadPoolName;
        private final int threadsSize;
        private final int taskQueueSize;

        private ThreadPoolConfig(Builder builder) {
            this.threadPoolName = builder.threadPoolName;
            this.threadsSize = builder.threadsSize;
            this.taskQueueSize = builder.taskQueueSize;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private String threadPoolName;
            private int threadsSize;
            private int taskQueueSize;

            public Builder threadPoolName(String threadPoolName) {
                this.threadPoolName = threadPoolName;
                return this;
            }

            public Builder threadsSize(int threadsSize) {
                this.threadsSize = threadsSize;
                return this;
            }

            public Builder taskQueueSize(int taskQueueSize) {
                this.taskQueueSize = taskQueueSize;
                return this;
            }

            public ThreadPoolConfig build() {
                return new ThreadPoolConfig(this);
            }
        }
    }
}