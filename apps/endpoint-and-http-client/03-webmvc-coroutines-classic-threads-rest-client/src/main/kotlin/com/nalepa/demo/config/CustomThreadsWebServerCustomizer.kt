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
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnThreading(Threading.PLATFORM)
class CustomThreadsWebServerCustomizer(
    private val tomcatServerProperties: TomcatServerProperties,
    private val executorsFactory: ExecutorsFactory,
) : WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory>, SmartLifecycle {


    private val executor = getExecutor()

    override fun customize(factory: ConfigurableTomcatWebServerFactory) {
        factory.addProtocolHandlerCustomizers(
            TomcatProtocolHandlerCustomizer { protocolHandler: ProtocolHandler ->
                protocolHandler.executor =
                    executorsFactory.monitorExecutorService(
                        executor,
                        "Http server pending request took:",
                        "custom.http.server.pending"
                    )
            })
    }

    private fun getExecutor(): ThreadPoolExecutor {
        // by default, Java minimize resources used when dealing with threads, so threads are created when there is reached queue limit
        // this task queue enforce java to create threads, when there are only elements in queue
        val taskQueue = TaskQueue(tomcatServerProperties.threads.maxQueueCapacity)
        val threadFactory = TaskThreadFactory("custom-tomcat-handler-", true, Thread.MAX_PRIORITY)

        tomcatServerProperties.acceptCount

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

        return executor
    }

    private var running = true

    override fun start() {
    }

    override fun stop() {
        shutdownExecutor()
        running = false
    }

    override fun isRunning(): Boolean = running

    private fun shutdownExecutor() {
        executor.shutdown()
        val isTerminated = executor.awaitTermination(5, TimeUnit.SECONDS)

        if (!isTerminated) {
            executor.shutdownNow()
        }
    }

}