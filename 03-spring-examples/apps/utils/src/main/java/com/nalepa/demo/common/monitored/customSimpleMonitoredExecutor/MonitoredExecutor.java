package com.nalepa.demo.common.monitored.customSimpleMonitoredExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.Executor;

// add here metric about tasks in queue?
public class MonitoredExecutor implements Executor {

    private final Executor delegate;
    public final String messagePrefix;
    public final String metricName;
    public final MeterRegistry meterRegistry;

    public MonitoredExecutor(Executor delegate, String messagePrefix, String metricName, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.messagePrefix = messagePrefix;
        this.metricName = metricName;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void execute(Runnable runnable) {
        delegate.execute(new MonitoredRunnable(runnable, messagePrefix, metricName, meterRegistry));
    }
}