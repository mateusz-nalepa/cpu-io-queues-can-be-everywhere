package com.nalepa.demo.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.connection.TransportSettings;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;

public class NettyMongoEventLoop implements MongoClientSettingsBuilderCustomizer, DisposableBean {


    private final ObjectProvider<MongoClientSettings> settings;
    private final MongoClientProperties properties;

    private volatile @Nullable EventLoopGroup eventLoopGroup;
    private final ObjectProvider<MeterRegistry> meterRegistry;

    NettyMongoEventLoop(
            ObjectProvider<MongoClientSettings> settings,
            ObjectProvider<MeterRegistry> meterRegistry,
            MongoClientProperties properties
    ) {
        this.settings = settings;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void customize(MongoClientSettings.@NonNull Builder builder) {
        MeterRegistry meterRegistry = this.meterRegistry.getIfAvailable();
        if (!properties.getEventLoopLag().isEnabled() || meterRegistry == null) {
            return;
        }
        if (!isCustomTransportConfiguration(this.settings.getIfAvailable())) {
            EventLoopGroup configuredEventLoopGroup = this.eventLoopGroup;
            if (configuredEventLoopGroup == null) {
                configuredEventLoopGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
                this.eventLoopGroup = configuredEventLoopGroup;
                new EventLoopLagMonitor(
                        meterRegistry,
                        properties.getEventLoopLag().getInterval(),
                        "eventLoop"
                ).registerGroup(configuredEventLoopGroup);
            }
            builder.transportSettings(
                    TransportSettings.nettyBuilder().eventLoopGroup(configuredEventLoopGroup).build()
            );
        }
    }

    @Override
    public void destroy() {
        EventLoopGroup eventLoopGroup = this.eventLoopGroup;
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
            this.eventLoopGroup = null;
        }
    }

    private boolean isCustomTransportConfiguration(@Nullable MongoClientSettings settings) {
        return settings != null && settings.getTransportSettings() != null;
    }

}
