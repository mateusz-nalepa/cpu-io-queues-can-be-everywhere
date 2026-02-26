package com.nalepa.demo.config;

import com.nalepa.demo.common.monitored.ExecutorsFactory;
import org.apache.tomcat.util.threads.TaskQueue;
import org.apache.tomcat.util.threads.TaskThreadFactory;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.thread.Threading;
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnThreading(Threading.PLATFORM)
public class CustomThreadsWebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> {

    private final TomcatServerProperties tomcatServerProperties;
    private final ExecutorsFactory executorsFactory;
    private final CustomTomcatThreadsShutdownManager customTomcatThreadsShutdownManager;

    public CustomThreadsWebServerCustomizer(
            TomcatServerProperties tomcatServerProperties,
            ExecutorsFactory executorsFactory,
            CustomTomcatThreadsShutdownManager customTomcatThreadsShutdownManager
    ) {
        this.tomcatServerProperties = tomcatServerProperties;
        this.executorsFactory = executorsFactory;
        this.customTomcatThreadsShutdownManager = customTomcatThreadsShutdownManager;
    }

    @Override
    public void customize(ConfigurableTomcatWebServerFactory factory) {
        ExecutorService customTomcatExecutor = executorsFactory.create(
                ExecutorsFactory.ThreadPoolConfig.builder()
                        .threadPoolName("custom.tomcat")
                        .threadsSize(tomcatServerProperties.getThreads().getMax())
                        .taskQueueSize(tomcatServerProperties.getThreads().getMaxQueueCapacity())
                        .build()
        );

        customTomcatThreadsShutdownManager.assignExecutor(customTomcatExecutor);

        factory.addProtocolHandlerCustomizers(
                protocolHandler ->
                        protocolHandler.setExecutor(customTomcatExecutor)
        );
    }

    // that's how Tomcat by default creates executor for handling requests
    private ThreadPoolExecutor getExecutor() {
        // by default, Java minimize resources used when dealing with threads, so threads are created when there is reached queue limit
        // this task queue enforce java to create threads, when there are only elements in queue
        TaskQueue taskQueue = new TaskQueue(tomcatServerProperties.getThreads().getMaxQueueCapacity());
        TaskThreadFactory threadFactory = new TaskThreadFactory("custom-tomcat-handler-", true, Thread.MAX_PRIORITY);

        tomcatServerProperties.getAcceptCount();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                tomcatServerProperties.getThreads().getMinSpare(),
                tomcatServerProperties.getThreads().getMax(),
                Duration.ofSeconds(60).toMillis(),
                TimeUnit.MILLISECONDS,
                taskQueue,
                threadFactory
        );
        taskQueue.setParent(executor);
        executor.prestartAllCoreThreads();

        return executor;
    }
}

@Component
class CustomTomcatThreadsShutdownManager implements SmartLifecycle {

    private volatile boolean running = true;
    private ExecutorService executorService;

    public void assignExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        shutdownExecutor();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // close custom executor after Tomcat is closed
        // so there will be no requests stopped in the middle of the execution
        return Integer.MIN_VALUE;
    }

    private void shutdownExecutor() {
        try {
            executorService.shutdown();
            boolean isTerminated = executorService.awaitTermination(5, TimeUnit.SECONDS);
            if (!isTerminated) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }
}