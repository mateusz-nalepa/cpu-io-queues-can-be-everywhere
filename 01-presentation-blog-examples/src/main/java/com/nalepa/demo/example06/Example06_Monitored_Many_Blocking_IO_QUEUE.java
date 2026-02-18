package com.nalepa.demo.example06;

import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedList;

public class Example06_Monitored_Many_Blocking_IO_QUEUE {
    public static void main(String[] args) {
        LinkedList<Runnable> queue = new LinkedList<>();

        Runnable monitoredRunnable1 = new MonitoredRunnable(() -> simulateBlockingIO(1));
        Runnable monitoredRunnable2 = new MonitoredRunnable(() -> simulateBlockingIO(2));
        Runnable monitoredRunnable3 = new MonitoredRunnable(() -> simulateBlockingIO(3));

        queue.add(monitoredRunnable1);
        queue.add(monitoredRunnable2);
        queue.add(monitoredRunnable3);

        while (!queue.isEmpty()) {
            Runnable runnable = queue.poll();
            runnable.run();
        }
    }

    static void simulateBlockingIO(int index) {
        log("Start blocking IO code for: " + index);
        nonThrowingSleep(5000); // http call, database call, reading from file, whatever what needs external data
        log("End blocking IO code for: " + index);
    }

    static void log(String message) {
        System.out.println(Thread.currentThread().getName() + " : " + LocalTime.now() + " : " + message);
    }

    static void nonThrowingSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
