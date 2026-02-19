package com.nalepa.demo.common.monitored;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class ExecutorsShutdownManager implements SmartLifecycle {

    private final List<ExecutorService> createdExecutors =
            Collections.synchronizedList(new ArrayList<>());

    private volatile boolean running = true;

    public void addExecutorService(ExecutorService executorService) {
        createdExecutors.add(executorService);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        shutdownExecutors();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void shutdownExecutors() {
        // 1. signal to end executors to end
        createdExecutors.forEach(ExecutorService::shutdown);

        // 2. parallel wait
        List<CompletableFuture<Map.Entry<Boolean, ExecutorService>>> futures = createdExecutors.stream()
                .map(executor -> CompletableFuture.supplyAsync(() -> {
                    try {
                        boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
                        return Map.entry(terminated, executor);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return Map.entry(false, executor);
                    }
                }))
                .toList();

        // 3. check if executors are finished or force shutdown
        futures.forEach(future -> {
            try {
                var entry = future.get();
                if (!entry.getKey()) {
                    entry.getValue().shutdownNow();
                }
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}