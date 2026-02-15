package com.nalepa.demo.config

import io.micrometer.core.instrument.MeterRegistry
import io.netty.util.concurrent.EventExecutor
import io.netty.util.concurrent.EventExecutorGroup
import org.springframework.boot.reactor.netty.NettyServerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import reactor.netty.http.server.HttpServer
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Configuration
class NettyEventLoopGroupMonitoring {

    @Bean
    fun eventLoopLagMonitor(meterRegistry: MeterRegistry): EventLoopLagMonitor =
        EventLoopLagMonitor(meterRegistry)
}

@Component
class EventLoopLagCustomizer(
    private val eventLoopLagMonitor: EventLoopLagMonitor,
) : NettyServerCustomizer {

    override fun apply(server: HttpServer): HttpServer {
        return server.doOnChannelInit { _, channel, _ ->
            val group = channel.eventLoop().parent()
            eventLoopLagMonitor.registerGroup(group)
        }
    }
}

class EventLoopLagMonitor(
    private val meterRegistry: MeterRegistry,
) {

    private val interval = Duration.ofMillis(100)

    private val registeredEventExecutors = ConcurrentHashMap.newKeySet<EventExecutor>()

    fun registerGroup(group: EventExecutorGroup) {
        group.forEach { executor ->
            registerExecutor(executor)
        }
    }

    private fun registerExecutor(executor: EventExecutor) {
        if (!registeredEventExecutors.add(executor)) {
            return
        }

        monitor(executor)
    }

    private fun monitor(executor: EventExecutor) {
        var last = System.nanoTime()

        executor.scheduleAtFixedRate({
            executor.execute {
                val now = System.nanoTime()
                val diff = now - last
                last = now

                val lagNanos = diff - interval.toNanos()

                if (lagNanos > 0) {
                    meterRegistry
                        .timer("custom.netty.eventloop.lag", "type", "server")
                        .record(lagNanos, TimeUnit.NANOSECONDS)
                }
            }
        }, 0, interval.toMillis(), TimeUnit.MILLISECONDS)
    }
}
