package com.nalepa.demo.common.monitored.customSimpleMonitoredExecutor;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MonitoredExecutorService implements ExecutorService {

    private final ExecutorService delegate;
    private final String losMessagePrefix;
    private final String name;
    private final MeterRegistry meterRegistry;

    public MonitoredExecutorService(ExecutorService delegate, String losMessagePrefix, String name, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.losMessagePrefix = losMessagePrefix;
        this.name = name;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(new MonitoredCallable<>(task, losMessagePrefix, name, meterRegistry));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(new MonitoredRunnable(task, losMessagePrefix, name, meterRegistry), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(new MonitoredRunnable(task, losMessagePrefix, name, meterRegistry));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks.stream()
                .map(t -> new MonitoredCallable<>(t, losMessagePrefix, name, meterRegistry))
                .collect(Collectors.toList()));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks.stream()
                .map(t -> new MonitoredCallable<>(t, losMessagePrefix, name, meterRegistry))
                .collect(Collectors.toList()), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks.stream()
                .map(t -> new MonitoredCallable<>(t, losMessagePrefix, name, meterRegistry))
                .collect(Collectors.toList()));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks.stream()
                .map(t -> new MonitoredCallable<>(t, losMessagePrefix, name, meterRegistry))
                .collect(Collectors.toList()), timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(new MonitoredRunnable(command, losMessagePrefix, name, meterRegistry));
    }
}