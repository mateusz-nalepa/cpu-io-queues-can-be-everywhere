package com.nalepa.demo.common.monitored.customMonitoredExecutor

import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


class MonitoredExecutorService(
    private val delegate: ExecutorService,
    private val losMessagePrefix: String,
    private val name: String,
    private val meterRegistry: MeterRegistry,
) : ExecutorService {
    override fun shutdown() {
        return delegate.shutdown()
    }

    override fun shutdownNow(): List<Runnable> {
        return delegate.shutdownNow()
    }

    override fun isShutdown(): Boolean {
        return delegate.isShutdown
    }

    override fun isTerminated(): Boolean {
        return delegate.isTerminated
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        return delegate.awaitTermination(timeout, unit)
    }

    override fun <T> submit(task: Callable<T>): Future<T> {
        return delegate.submit(MonitoredCallable(task, losMessagePrefix, name, meterRegistry))
    }

    override fun <T> submit(task: Runnable, result: T): Future<T> {
        return delegate.submit(MonitoredRunnable(task, losMessagePrefix, name, meterRegistry), result)
    }

    override fun submit(task: Runnable): Future<*> {
        return delegate.submit(MonitoredRunnable(task, losMessagePrefix, name, meterRegistry))
    }

    override fun <T> invokeAll(tasks: Collection<Callable<T>>): List<Future<T>> {
        return delegate.invokeAll(tasks.map { MonitoredCallable(it, losMessagePrefix, name, meterRegistry) })
    }

    override fun <T> invokeAll(
        tasks: Collection<Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): List<Future<T>> {
        return delegate.invokeAll(
            tasks.map { MonitoredCallable(it, losMessagePrefix, name, meterRegistry) },
            timeout,
            unit
        )
    }

    override fun <T> invokeAny(tasks: Collection<Callable<T>>): T {
        return delegate.invokeAny(tasks.map { MonitoredCallable(it, losMessagePrefix, name, meterRegistry) })
    }

    override fun <T> invokeAny(
        tasks: Collection<Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): T {
        return delegate.invokeAny(
            tasks.map { MonitoredCallable(it, losMessagePrefix, name, meterRegistry) },
            timeout,
            unit
        )
    }

    override fun execute(command: Runnable) {
        delegate.execute(MonitoredRunnable(command, losMessagePrefix, name, meterRegistry))
    }
}