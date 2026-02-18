
package com.nalepa.demo.example12;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.LinkedBlockingQueue;

public class example12_Without_SEDA {
    public static void main(String[] args) throws InterruptedException {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        long exampleStart = System.nanoTime();

        long startTimeForIndex1 = System.nanoTime();
        queue.add(new MonitoredRunnable(1, () -> {
            byte[] responseFromFirstClient = ioHttpClientCall(1);
            cpuJsonParsing(responseFromFirstClient);
            log("######## Data ready for index 1. It took: " +
                    Duration.ofNanos(System.nanoTime() - startTimeForIndex1).toSeconds() + " s");
        }));

        long startTimeForIndex2 = System.nanoTime();
        queue.add(new MonitoredRunnable(2, () -> {
            byte[] responseFromFirstClient = ioHttpClientCall(2);
            cpuJsonParsing(responseFromFirstClient);
            log("######## Data ready for index 2. It took: " +
                    Duration.ofNanos(System.nanoTime() - startTimeForIndex2).toSeconds() + " s");
        }));

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

    static byte[] ioHttpClientCall(int index) {
        log("Start getting data for index: " + index);
        try {
            Thread.sleep(Duration.ofSeconds(3).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log("Data ready for index: " + index);
        return ("data with index: " + index).getBytes();
    }

    static void cpuJsonParsing(byte[] byteArray) {
        log("Start parsing data for " + new String(byteArray));
        try {
            Thread.sleep(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log("End parsing data for " + new String(byteArray));
    }

    static void log(String message) {
        System.out.println(LocalTime.now() + " : " + Thread.currentThread().getName() + " : " + message);
    }

    static class MonitoredRunnable implements Runnable {
        private final int index;
        private final Runnable delegate;
        private final long runnableInstanceCreatedAt;

        public MonitoredRunnable(int index, Runnable delegate) {
            this.index = index;
            this.delegate = delegate;
            this.runnableInstanceCreatedAt = System.nanoTime();
        }

        @Override
        public void run() {
            System.out.println();
            log("Index: " + index + ". Queue wait time took: " +
                    Duration.ofNanos(System.nanoTime() - runnableInstanceCreatedAt).toSeconds() + " s");
            long startExecution = System.nanoTime();
            delegate.run();
            log("Index: " + index + ". Task execution took: " +
                    Duration.ofNanos(System.nanoTime() - startExecution).toSeconds() + " s");
        }
    }
}
