package com.nalepa.demo.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class EventLoopLagMonitor {

    private static final String DEFAULT_METRIC_NAME = "custom.netty.eventloop.lag";

    private final MeterRegistry meterRegistry;
    private final long intervalNanos;
    private final String metricName;

    public EventLoopLagMonitor(MeterRegistry meterRegistry) {
        this(meterRegistry, Duration.ofMillis(20), DEFAULT_METRIC_NAME);
    }

    EventLoopLagMonitor(MeterRegistry meterRegistry, Duration interval, String metricName) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.intervalNanos = interval.toNanos();
        if (intervalNanos <= 0) {
            throw new IllegalArgumentException("interval must be positive");
        }
        this.metricName = Objects.requireNonNull(metricName, "metricName");
    }

    public void registerGroup(EventExecutorGroup group) {
        group.forEach(this::monitor);
    }

    private void monitor(EventExecutor executor) {
        AtomicLong last = new AtomicLong(System.nanoTime());

        executor.scheduleAtFixedRate(() -> {
                    long now = System.nanoTime();
                    long diff = now - last.getAndSet(now);

                    long lagNanos = diff - intervalNanos;

                    if (lagNanos > 0) {
                        meterRegistry
                                .timer(metricName,
                                        "executor", Thread.currentThread().getName()
                                )
                                .record(lagNanos, TimeUnit.NANOSECONDS);
                    }
                },
                0, intervalNanos, TimeUnit.NANOSECONDS
        );
    }
}
