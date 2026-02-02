package com.nalepa.demo.common.monitored.customMonitoredExecutor

import com.nalepa.demo.common.DummyLogger
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration
import java.util.concurrent.Callable

class MonitoredRunnable internal constructor(
    private val delegate: Runnable,
    val messagePrefix: String,
    val name: String,
    val meterRegistry: MeterRegistry,
) : Runnable {
    private val startTime = System.nanoTime()

    private val waitTimer: Timer =
        Timer
            .builder("monitored.task.wait.time")
            .tag("name", name)
            .tag("taskType", "runnable")
            .description("Time between creation and execution")
            .register(meterRegistry)

    override fun run() {
        val duration = Duration.ofNanos(System.nanoTime() - startTime)
        DummyLogger.log(this, "$messagePrefix $duration")
        waitTimer.record(duration)

        delegate.run()
    }
}

class MonitoredCallable<T> internal constructor(
    private val delegate: Callable<T>,
    val messagePrefix: String,
    val name: String,
    val meterRegistry: MeterRegistry,
) : Callable<T> {
    private val startTime = System.nanoTime()

    private val waitTimer: Timer =
        Timer
            .builder("monitored.task.wait.time")
            .tag("name", name)
            .tag("taskType", "callable")
            .description("Time between creation and execution")
            .register(meterRegistry)

    override fun call(): T {
        val duration = Duration.ofNanos(System.nanoTime() - startTime)
        DummyLogger.log(this, "$messagePrefix $duration")
        waitTimer.record(duration)

        return delegate.call()
    }
}

