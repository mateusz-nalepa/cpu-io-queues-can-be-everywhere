package com.nalepa.demo.common.monitored.customSimpleMonitoredExecutor;


import com.nalepa.demo.common.DummyLogger;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

public class MonitoredRunnable implements Runnable {

    private final Runnable delegate;
    public final String messagePrefix;
    public final String name;
    public final MeterRegistry meterRegistry;

    private final long startTime = System.nanoTime();

    private final Timer waitTimer;

    MonitoredRunnable(Runnable delegate, String messagePrefix, String name, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.messagePrefix = messagePrefix;
        this.name = name;
        this.meterRegistry = meterRegistry;

        this.waitTimer = Timer
                .builder("monitored.task.wait.time")
                .tag("name", name)
                .tag("taskType", "runnable")
                .description("Time between creation and execution")
                .register(meterRegistry);

        // TODO: read values from Thread Locals and save them to map
    }

    @Override
    public void run() {
        Duration duration = Duration.ofNanos(System.nanoTime() - startTime);
        DummyLogger.log(this, messagePrefix + " " + duration);
        waitTimer.record(duration);
        try {
            // TODO:
//        put values from map to thread locals
            delegate.run();
        } finally {
            // TODO: clear thread locals
        }
    }
}