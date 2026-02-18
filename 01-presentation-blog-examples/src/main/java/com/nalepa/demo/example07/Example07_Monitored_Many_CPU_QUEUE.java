package com.nalepa.demo.example07;

import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedList;

public class Example07_Monitored_Many_CPU_QUEUE {
    public static void main(String[] args) {
        LinkedList<Runnable> queue = new LinkedList<>();

        Runnable monitoredRunnable1 = new MonitoredRunnable(() -> simulateCpuCode(1));
        Runnable monitoredRunnable2 = new MonitoredRunnable(() -> simulateCpuCode(2));
        Runnable monitoredRunnable3 = new MonitoredRunnable(() -> simulateCpuCode(3));

        queue.add(monitoredRunnable1);
        queue.add(monitoredRunnable2);
        queue.add(monitoredRunnable3);

        while (!queue.isEmpty()) {
            Runnable runnable = queue.poll();
            runnable.run();
        }
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
