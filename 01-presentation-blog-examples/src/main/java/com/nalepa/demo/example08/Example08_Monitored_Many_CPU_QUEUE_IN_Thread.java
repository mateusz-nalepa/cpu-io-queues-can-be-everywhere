package com.nalepa.demo.example08;

import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedList;

public class Example08_Monitored_Many_CPU_QUEUE_IN_Thread {
    public static void main(String[] args) throws InterruptedException {
        LinkedList<Runnable> queue = new LinkedList<>();

        Runnable monitoredRunnable1 = new MonitoredRunnable(() -> simulateCpuCode(1));
        Runnable monitoredRunnable2 = new MonitoredRunnable(() -> simulateCpuCode(2));
        Runnable monitoredRunnable3 = new MonitoredRunnable(() -> simulateCpuCode(3));

        queue.add(monitoredRunnable1);
        queue.add(monitoredRunnable2);
        queue.add(monitoredRunnable3);

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
    }

    static long simulateCpuCode(int index) {
        log("Start CPU code for: " + index);
        long startTime = System.nanoTime();
        long iteration = 0L;
        while (Duration.ofNanos(System.nanoTime() - startTime).getSeconds() < 5) {
            iteration++;
            Math.sqrt((double) iteration);
        }
        log("End CPU code for: " + index);
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
            log("Created MonitoredRunnable instance");
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
