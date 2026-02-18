package com.nalepa.demo.example10;

import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedList;

public class Example10_Without_Bulkhead_Pattern {
    public static void main(String[] args) throws InterruptedException {
        LinkedList<Runnable> queue = new LinkedList<>();
        long exampleStart = System.nanoTime();

        queue.add(new MonitoredRunnable(() -> simulateSlowEndpoint(1)));
        queue.add(new MonitoredRunnable(() -> simulateSlowEndpoint(2)));
        queue.add(new MonitoredRunnable(() -> simulateSlowEndpoint(3)));

        queue.add(new MonitoredRunnable(() -> simulateFastEndpoint(1)));
        queue.add(new MonitoredRunnable(() -> simulateFastEndpoint(2)));
        queue.add(new MonitoredRunnable(() -> simulateFastEndpoint(3)));

        Thread workerThread = new Thread(
                () -> {
                    while (!queue.isEmpty()) {
                        Runnable runnable = queue.poll();
                        runnable.run();
                    }
                },
                "worker-thread"
        );
        workerThread.start();
        workerThread.join();
        log("Example ended after: " + Duration.ofNanos(System.nanoTime() - exampleStart).toSeconds() + " s");
    }

    static void simulateSlowEndpoint(int index) {
        log("Start slow for: " + index);
        try {
            Thread.sleep(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
