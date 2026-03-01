package com.nalepa.demo.example09;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Example09_Monitored_BLOCKING_IO_vs_CPU_BOUND_IN_EXECUTOR {
    public static void main(String[] args) throws Exception {
        ThreadFactory threadFactory = new SimpleThreadFactory("workerThread");
        int cpuCount = Runtime.getRuntime().availableProcessors();

        var executor =
                new ThreadPoolExecutor(
                        cpuCount,
                        cpuCount,
                        0L, MILLISECONDS,
                        new LinkedBlockingQueue<>(10000),
                        threadFactory
                );
        var futures = new ArrayList<Future<?>>();

        for (int index = 0; index < 10000; index++) {
            futures.add(executor.submit(new MonitoredRunnable(() -> simulateBlockingIO())));
            // or
//             futures.add(executor.submit(new MonitoredRunnable(() -> simulateCpuCode())));
        }

        for (Future<?> future : futures) {
            future.get();
        }
    }

    static void simulateBlockingIO() {
        try {
            Thread.sleep(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static long simulateCpuCode() {
        long startTime = System.nanoTime();
        long iteration = 0L;
        while (Duration.ofNanos(System.nanoTime() - startTime).getSeconds() < 5) {
            iteration++;
            Math.sqrt((double) iteration);
        }
        return iteration;
    }

    static void log(String message) {
        System.out.println(Thread.currentThread().getName() + " : " + LocalTime.now() + " : " + message);
    }

    static class MonitoredRunnable implements Runnable {
        private final Runnable delegate;
        private final long runnableInstanceCreatedAt;

        public MonitoredRunnable(Runnable delegate) {
            this.delegate = delegate;
            this.runnableInstanceCreatedAt = System.nanoTime();
        }

        @Override
        public void run() {
            log("Queue wait time took: " + Duration.ofNanos(System.nanoTime() - runnableInstanceCreatedAt).toSeconds() + " s");
            long startExecution = System.nanoTime();
            delegate.run();
            log("Task execution took: " + Duration.ofNanos(System.nanoTime() - startExecution).toSeconds() + " s");
        }
    }

    static class SimpleThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public SimpleThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, namePrefix + "-" + threadNumber.getAndIncrement());
        }
    }
}
