package com.nalepa.demo.example13;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Example13_With_SEDA {
    public static void main(String[] args) throws InterruptedException {
        LinkedBlockingQueue<Runnable> queueForIo = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<Runnable> queueForCPU = new LinkedBlockingQueue<>();

        AtomicInteger counterOfCPUTasks = new AtomicInteger(2); // used to wait for all CPU tasks to finish before ending the example
        long exampleStart = System.nanoTime();


        long startTimeForIndex1 = System.nanoTime();
        queueForIo.add(new MonitoredRunnable(1, () -> {
            byte[] byteArrayFromClient = ioHttpClientCall(1);
            queueForCPU.add(new MonitoredRunnable(1, () -> {
                cpuJsonParsing(byteArrayFromClient);
                log("######## Data ready for index 1. It took: " +
                        Duration.ofNanos(System.nanoTime() - startTimeForIndex1).toSeconds() + " s");
            }));
        }));

        long startTimeForIndex2 = System.nanoTime();
        queueForIo.add(new MonitoredRunnable(2, () -> {
            byte[] byteArrayFromClient = ioHttpClientCall(2);
            queueForCPU.add(new MonitoredRunnable(2, () -> {
                cpuJsonParsing(byteArrayFromClient);
                log("######## Data ready for index 2. It took: " +
                        Duration.ofNanos(System.nanoTime() - startTimeForIndex2).toSeconds() + " s");
            }));
        }));

        Thread ioThread = new Thread(
                () -> {
                    while (!queueForIo.isEmpty()) {
                        Runnable runnable = queueForIo.poll();
                        runnable.run();
                    }
                },
                "io-thread"
        );

        Thread cpuThread = new Thread(
                () -> {
                    while (!queueForCPU.isEmpty() || counterOfCPUTasks.get() != 0) {
                        Runnable runnable = queueForCPU.poll();
                        if (runnable != null) {
                            runnable.run();
                            counterOfCPUTasks.decrementAndGet();
                        }
                    }
                },
                "cpu-thread"
        );

        ioThread.start();
        cpuThread.start();

        ioThread.join();
        cpuThread.join();

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
