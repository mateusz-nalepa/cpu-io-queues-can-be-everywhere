package com.nalepa.demo.config

import com.nalepa.demo.common.monitored.ExecutorsFactory
import org.apache.coyote.ProtocolHandler
import org.apache.tomcat.util.threads.TaskQueue
import org.apache.tomcat.util.threads.TaskThreadFactory
import org.apache.tomcat.util.threads.ThreadPoolExecutor
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading
import org.springframework.boot.thread.Threading
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnThreading(Threading.PLATFORM)
class CustomThreadsWebServerCustomizer(
    private val tomcatServerProperties: TomcatServerProperties,
    private val executorsFactory: ExecutorsFactory,
    private val customTomcatThreadsShutdownManager: CustomTomcatThreadsShutdownManager,
) : WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> {

    override fun customize(factory: ConfigurableTomcatWebServerFactory) {

        val customTomcatExecutor =
            executorsFactory
                .create(
                    ExecutorsFactory.ThreadPoolConfig.builder()
                        .threadPoolName("custom.tomcat")
                        .threadsSize(tomcatServerProperties.getThreads().getMax())
                        .taskQueueSize(tomcatServerProperties.getThreads().getMaxQueueCapacity())
                        .build()
                )

        customTomcatThreadsShutdownManager.assignExecutor(customTomcatExecutor)

        factory.addProtocolHandlerCustomizers(
            TomcatProtocolHandlerCustomizer { protocolHandler: ProtocolHandler ->
                protocolHandler.executor = customTomcatExecutor
            })
    }

    // that's how Tomcat by default creates executor for handling requests
    private fun getExecutor(): ThreadPoolExecutor {
        // by default, Java minimize resources used when dealing with threads, so threads are created when there is reached queue limit
        // this task queue enforce java to create threads, when there are only elements in queue
        val taskQueue = TaskQueue(tomcatServerProperties.threads.maxQueueCapacity)
        val threadFactory = TaskThreadFactory("custom-tomcat-handler-", true, Thread.MAX_PRIORITY)

        val executor =
            ThreadPoolExecutor(
                tomcatServerProperties.threads.minSpare,
                tomcatServerProperties.threads.max,
                Duration.ofSeconds(60).toMillis(),
                TimeUnit.MILLISECONDS,
                taskQueue,
                threadFactory,
            )
        taskQueue.setParent(executor)
        executor.prestartAllCoreThreads()

        // note
        // com.nalepa.demo.common.monitored.customSimpleMonitoredExecutorMonitoredExecutorService
        // can be used here in order to add queue wait time for this Pool
        // ThreadPoolExecutor from Tomcat extends ExecutorService

        return executor
    }
}

@Component
class CustomTomcatThreadsShutdownManager : SmartLifecycle {

    private var running = true

    private lateinit var executorService: ExecutorService

    fun assignExecutor(executorService: ExecutorService) {
        this.executorService = executorService
    }

    override fun start() {
    }

    override fun stop() {
        shutdownExecutor()
        running = false
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int {
        // close custom executor after Tomcat is closed
        // so there will be no requests stopped in the middle of the execution
        return Integer.MIN_VALUE
    }

    private fun shutdownExecutor() {
        executorService.shutdown()
        val isTerminated = executorService.awaitTermination(5, TimeUnit.SECONDS)

        if (!isTerminated) {
            executorService.shutdownNow()
        }
    }

}