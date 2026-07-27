package com.nalepa.demo.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class EventLoopLagMonitor {

    private final MeterRegistry meterRegistry;
    private final Duration interval = Duration.ofMillis(20);

    public EventLoopLagMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void registerGroup(EventExecutorGroup group) {
        group.forEach(this::monitor);
    }

    private void monitor(EventExecutor executor) {
        AtomicLong last = new AtomicLong(System.nanoTime());

        executor.scheduleAtFixedRate(() -> {
                    long now = System.nanoTime();
                    long diff = now - last.getAndSet(now);

                    long lagNanos = diff - interval.toNanos();

                    if (lagNanos > 0) {
                        meterRegistry
                                .timer("custom.netty.eventloop.lag",
                                        "executor", Thread.currentThread().getName()
                                )
                                .record(lagNanos, TimeUnit.NANOSECONDS);
                    }
                },
                0, interval.toMillis(), TimeUnit.MILLISECONDS
        );
    }
}
