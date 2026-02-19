package com.nalepa.demo.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import org.springframework.boot.reactor.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class NettyEventLoopGroupMonitoring {

    @Bean
    public EventLoopLagMonitor eventLoopLagMonitor(MeterRegistry meterRegistry) {
        return new EventLoopLagMonitor(meterRegistry);
    }

    @Component
    public static class EventLoopLagCustomizer implements NettyServerCustomizer {

        private final EventLoopLagMonitor eventLoopLagMonitor;

        public EventLoopLagCustomizer(EventLoopLagMonitor eventLoopLagMonitor) {
            this.eventLoopLagMonitor = eventLoopLagMonitor;
        }

        @Override
        public HttpServer apply(HttpServer server) {
            return server.doOnChannelInit((observer, channel, remoteAddress) -> {
                EventExecutorGroup group = channel.eventLoop().parent();
                eventLoopLagMonitor.registerGroup(group);
            });
        }
    }

    public static class EventLoopLagMonitor {

        private final MeterRegistry meterRegistry;
        private final Duration interval = Duration.ofMillis(100);
        private final Set<EventExecutor> registeredEventExecutors = ConcurrentHashMap.newKeySet();

        public EventLoopLagMonitor(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        public void registerGroup(EventExecutorGroup group) {
            group.forEach(this::registerExecutor);
        }

        private void registerExecutor(EventExecutor executor) {
            if (!registeredEventExecutors.add(executor)) {
                return;
            }
            monitor(executor);
        }

        private void monitor(EventExecutor executor) {
            AtomicLong last = new AtomicLong(System.nanoTime());

            executor.scheduleAtFixedRate(() ->
                            executor.execute(() -> {
                                long now = System.nanoTime();
                                long diff = now - last.getAndSet(now);

                                long lagNanos = diff - interval.toNanos();

                                if (lagNanos > 0) {
                                    meterRegistry
                                            .timer("custom.netty.eventloop.lag")
                                            .record(lagNanos, TimeUnit.NANOSECONDS);
                                }
                            }),
                    0, interval.toMillis(), TimeUnit.MILLISECONDS
            );
        }
    }
}