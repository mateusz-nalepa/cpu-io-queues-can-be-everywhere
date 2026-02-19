package com.nalepa.demo.common.monitored.customSimpleMonitoredExecutor;

import com.nalepa.demo.common.DummyLogger;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.Callable;

public class MonitoredCallable<T> implements Callable<T> {

    private final Callable<T> delegate;
    public final String messagePrefix;
    public final String name;
    public final MeterRegistry meterRegistry;

    private final long startTime = System.nanoTime();

    private final Timer waitTimer;

    MonitoredCallable(Callable<T> delegate, String messagePrefix, String name, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.messagePrefix = messagePrefix;
        this.name = name;
        this.meterRegistry = meterRegistry;

        this.waitTimer = Timer
                .builder("monitored.task.wait.time")
                .tag("name", name)
                .tag("taskType", "callable")
                .description("Time between creation and execution")
                .register(meterRegistry);
    }

    @Override
    public T call() throws Exception {
        Duration duration = Duration.ofNanos(System.nanoTime() - startTime);
        DummyLogger.log(this, messagePrefix + " " + duration);
        waitTimer.record(duration);

        return delegate.call();
    }
}