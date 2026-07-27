package com.nalepa.demo.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.connection.TransportSettings;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.stereotype.Component;

@Component
// this class is copy-paste from
// org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration.NettyDriverMongoClientSettingsBuilderCustomizer

// what is added here?
// customMonitorEventLoop
public class NettyMongoEventLoop implements MongoClientSettingsBuilderCustomizer, DisposableBean {


    private final ObjectProvider<MongoClientSettings> settings;

    private volatile @Nullable EventLoopGroup eventLoopGroup;
    private volatile @Nullable MeterRegistry meterRegistry;

    NettyMongoEventLoop(ObjectProvider<MongoClientSettings> settings, MeterRegistry meterRegistry) {
        this.settings = settings;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void customize(MongoClientSettings.Builder builder) {

        if (!isCustomTransportConfiguration(this.settings.getIfAvailable())) {
            EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

            customMonitorEventLoop(eventLoopGroup);

            this.eventLoopGroup = eventLoopGroup;
            builder.transportSettings(TransportSettings.nettyBuilder().eventLoopGroup(eventLoopGroup).build());
        }
    }

    private void customMonitorEventLoop(EventLoopGroup eventLoopGroup) {
        new EventLoopLagMonitor(meterRegistry)
                .registerGroup(eventLoopGroup);
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
