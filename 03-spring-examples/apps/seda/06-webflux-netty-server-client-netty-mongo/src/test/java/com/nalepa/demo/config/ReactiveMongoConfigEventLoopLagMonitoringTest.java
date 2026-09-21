package com.nalepa.demo.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.connection.NettyTransportSettings;
import com.mongodb.connection.TransportSettings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.channel.EventLoopGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ReactiveMongoConfigEventLoopLagMonitoringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReactiveMongoConfigEventLoopLagMonitoring.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void bindsEventLoopLagProperties() {
        this.contextRunner
                .withPropertyValues("mongo-client.event-loop-lag.interval=10ms")
                .run(context -> {
                    MongoClientProperties properties = context.getBean(MongoClientProperties.class);

                    assertThat(properties.getEventLoopLag().getInterval())
                            .isEqualTo(Duration.ofMillis(10));
                    assertThat(properties.getEventLoopLag().isEnabled()).isTrue();
                });
    }

    @Test
    void monitoringCanBeDisabled() {
        this.contextRunner
                .withPropertyValues("mongo-client.event-loop-lag.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(NettyMongoEventLoop.class));
    }

    @Test
    void customMongoClientSettingsArePreserved() {
        this.contextRunner
                .withUserConfiguration(CustomMongoSettingsConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean(NettyMongoEventLoop.class));
    }

    @Test
    void recordsPositiveEventLoopLagAndShutsDownTheGroup() throws InterruptedException {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ObjectProvider<MongoClientSettings> settings = beanFactory.getBeanProvider(MongoClientSettings.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        beanFactory.registerSingleton("meterRegistry", meterRegistry);
        ObjectProvider<MeterRegistry> meterRegistryProvider = beanFactory.getBeanProvider(MeterRegistry.class);
        MongoClientProperties properties = new MongoClientProperties();
        properties.getEventLoopLag().setInterval(Duration.ofMillis(10));
        NettyMongoEventLoop monitor = new NettyMongoEventLoop(settings, meterRegistryProvider, properties);
        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder();

        monitor.customize(settingsBuilder);

        NettyTransportSettings transportSettings =
                (NettyTransportSettings) settingsBuilder.build().getTransportSettings();
        EventLoopGroup eventLoopGroup = transportSettings.getEventLoopGroup();
        CountDownLatch taskStarted = new CountDownLatch(1);
        eventLoopGroup.next().execute(() -> {
            taskStarted.countDown();
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        });

        assertThat(taskStarted.await(1, TimeUnit.SECONDS)).isTrue();
        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    assertThat(meterRegistry.find("eventLoop").timers()).isNotEmpty();
                    meterRegistry.find("eventLoop").timers().forEach(timer -> {
                        assertThat(timer.getId().getTag("executor")).isNotBlank();
                        assertThat(timer.count()).isPositive();
                    });
                });

        monitor.destroy();

        assertThat(eventLoopGroup.isTerminated()).isTrue();
    }

    @Test
    void doesNotCustomizeWhenPropertiesDisableMonitoring() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ObjectProvider<MongoClientSettings> settings = beanFactory.getBeanProvider(MongoClientSettings.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        beanFactory.registerSingleton("meterRegistry", meterRegistry);
        ObjectProvider<MeterRegistry> meterRegistryProvider = beanFactory.getBeanProvider(MeterRegistry.class);
        MongoClientProperties properties = new MongoClientProperties();
        properties.getEventLoopLag().setEnabled(false);
        NettyMongoEventLoop monitor = new NettyMongoEventLoop(settings, meterRegistryProvider, properties);
        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder();

        monitor.customize(settingsBuilder);

        assertThat(settingsBuilder.build().getTransportSettings()).isNull();
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomMongoSettingsConfiguration {

        @Bean
        MongoClientSettings mongoClientSettings() {
            return MongoClientSettings.builder()
                    .transportSettings(TransportSettings.asyncBuilder().build())
                    .build();
        }
    }
}
