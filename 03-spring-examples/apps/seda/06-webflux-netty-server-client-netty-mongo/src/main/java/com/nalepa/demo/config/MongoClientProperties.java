package com.nalepa.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "mongo-client")
public class MongoClientProperties {

    @NestedConfigurationProperty
    private EventLoopLag eventLoopLag = new EventLoopLag();

    public EventLoopLag getEventLoopLag() {
        return eventLoopLag;
    }

    public void setEventLoopLag(EventLoopLag eventLoopLag) {
        this.eventLoopLag = Objects.requireNonNull(eventLoopLag, "eventLoopLag");
    }

    public static class EventLoopLag {

        private boolean enabled = true;
        private Duration interval = Duration.ofMillis(10);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            Duration configuredInterval = Objects.requireNonNull(interval, "interval");
            if (configuredInterval.isZero() || configuredInterval.isNegative()) {
                throw new IllegalArgumentException("interval must be positive");
            }
            this.interval = configuredInterval;
        }
    }
}
