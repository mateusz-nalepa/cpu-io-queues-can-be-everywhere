package com.nalepa.demo.common.monitored.customMonitoredExecutor

import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.Executor

// add here metric about tasks in queue?
class MonitoredExecutor(
    private val delegate: Executor,
    val messagePrefix: String,
    val metricName: String,
    val meterRegistry: MeterRegistry,
) : Executor {
    override fun execute(runnable: Runnable) {
        delegate.execute(MonitoredRunnable(runnable, messagePrefix, metricName, meterRegistry))
    }
}

