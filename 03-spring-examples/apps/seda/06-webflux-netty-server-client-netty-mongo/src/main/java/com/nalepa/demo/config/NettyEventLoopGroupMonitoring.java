package com.nalepa.demo.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.concurrent.EventExecutorGroup;
import org.springframework.boot.reactor.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import reactor.netty.http.server.HttpServer;

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
            // doOnBound it's like doOnStart
            return server.doOnBound((disposableServer) -> {
                EventExecutorGroup group = disposableServer.channel().eventLoop().parent();
                eventLoopLagMonitor.registerGroup(group);
            });
        }
    }

}
