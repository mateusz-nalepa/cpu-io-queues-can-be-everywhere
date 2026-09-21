package com.nalepa.demo.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@AutoConfiguration(
        before = MongoReactiveAutoConfiguration.class,
        afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration"
)
@ConditionalOnClass({MongoClient.class, Flux.class})
@EnableConfigurationProperties(MongoClientProperties.class)
public class ReactiveMongoConfigEventLoopLagMonitoring {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({
            MongoClientSettings.class,
            MeterRegistry.class,
            SocketChannel.class,
            MultiThreadIoEventLoopGroup.class,
            NioIoHandler.class
    })
    @ConditionalOnMissingBean(MongoClientSettings.class)
    @ConditionalOnProperty(
            prefix = "mongo-client.event-loop-lag",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    static class NettyDriverConfiguration {

        @Bean
        NettyMongoEventLoop nettyMongoEventLoop(
                ObjectProvider<MongoClientSettings> settings,
                ObjectProvider<MeterRegistry> meterRegistry,
                MongoClientProperties properties
        ) {
            return new NettyMongoEventLoop(settings, meterRegistry, properties);
        }
    }
}
