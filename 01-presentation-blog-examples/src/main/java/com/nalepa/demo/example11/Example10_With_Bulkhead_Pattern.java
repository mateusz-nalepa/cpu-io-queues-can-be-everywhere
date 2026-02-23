package com.nalepa.demo.example11;

import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedList;

public class Example10_With_Bulkhead_Pattern {
    public static void main(String[] args) throws InterruptedException {
        LinkedList<Runnable> queueForSlow = new LinkedList<>();
        LinkedList<Runnable> queueForFast = new LinkedList<>();
        long exampleStart = System.nanoTime();

        queueForSlow.add(new MonitoredRunnable(() -> simulateSlowEndpoint(1)));
        queueForSlow.add(new MonitoredRunnable(() -> simulateSlowEndpoint(2)));

        queueForFast.add(new MonitoredRunnable(() -> simulateFastEndpoint(1)));
        queueForFast.add(new MonitoredRunnable(() -> simulateFastEndpoint(2)));

        Thread threadForSlow = new Thread(
                () -> {
                    while (!queueForSlow.isEmpty()) {
                        Runnable runnable = queueForSlow.poll();
                        runnable.run();
                    }
                    log("#### Slow tasks finished! ####");
                },
                "thread-for-slow"
        );

        Thread threadForFast = new Thread(
                () -> {
                    while (!queueForFast.isEmpty()) {
                        Runnable runnable = queueForFast.poll();
                        runnable.run();
                    }
                    log("#### Fast tasks finished! ####");
                },
                "thread-for-fast"
        );

        threadForSlow.start();
        threadForFast.start();
        threadForSlow.join();
        threadForFast.join();

        log("Example ended after: " + Duration.ofNanos(System.nanoTime() - exampleStart).toSeconds() + " s");
    }

    static void simulateSlowEndpoint(int index) {
        log("Start slow for: " + index);
        try {
            Thread.sleep(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log("End slow for: " + index);
    }

    static void simulateFastEndpoint(int index) {
        log("Start fast for: " + index);
        log("End fast for: " + index);
    }

    static void log(String message) {
        System.out.println(LocalTime.now() + " : " + Thread.currentThread().getName() + " : " + message);
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
            System.out.println();
            log("Queue wait time took: " + Duration.ofNanos(System.nanoTime() - runnableInstanceCreatedAt).toSeconds() + " s");
            long startExecution = System.nanoTime();
            delegate.run();
            log("Task execution took: " + Duration.ofNanos(System.nanoTime() - startExecution).toSeconds() + " s");
        }
    }
}
