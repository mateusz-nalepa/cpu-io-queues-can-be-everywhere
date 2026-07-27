package com.nalepa.demo.example05;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VirtualThreadsExample {
    public static void main(String[] args) throws Exception {
        var carriersThreadCounts = 1;
        System.setProperty("jdk.virtualThreadScheduler.parallelism", String.valueOf(carriersThreadCounts));
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", String.valueOf(carriersThreadCounts));

        // this works as expected
        System.out.println("monitorTaskQueueWaitTime");
        monitorTaskQueueWaitTime(carriersThreadCounts);

        // this doesn't :<
        System.out.println();
        System.out.println("monitorVirtualThreadWaitsToBeMounted");
        monitorVirtualThreadWaitsToBeMounted(carriersThreadCounts);
    }

    private static void monitorTaskQueueWaitTime(int carriersThreadCounts) {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var monitoredExecutor = new MonitoredExecutor(executor);

        for (int index = 0; index < carriersThreadCounts; index++) {
            monitoredExecutor.execute(() -> simulateCpuCode(5), "task-cpu-" + index);
        }

        monitoredExecutor.execute(() -> simulateIO(5), "task-IO");


        runtimeThrowingSleep(12);
    }

    private static void monitorVirtualThreadWaitsToBeMounted(int carriersThreadCounts) {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var monitoredExecutor = new MonitoredExecutor(executor);

        monitoredExecutor.execute(() -> simulateIO(5), "task-IO");

        for (int index = 0; index < carriersThreadCounts; index++) {
            monitoredExecutor.execute(() -> simulateCpuCode(10), "task-cpu-" + index);
        }

        runtimeThrowingSleep(12);
    }

    static void simulateIO(int seconds) {
        runtimeThrowingSleep(seconds);
    }

    static long simulateCpuCode(int seconds) {
        long startTime = System.nanoTime();
        long iteration = 0L;
        while (Duration.ofNanos(System.nanoTime() - startTime).getSeconds() < seconds) {
            iteration++;
            Math.sqrt((double) iteration);
        }
        return iteration;
    }

    static void log(String message) {
        System.out.println(Thread.currentThread().getName() + " : " + LocalTime.now() + " : " + message);
    }

    static class MonitoredExecutor implements Executor {

        private final Executor delegate;

        MonitoredExecutor(Executor delegate) {
            this.delegate = delegate;
        }

        public void execute(Runnable command, String id) {
            execute(new MonitoredRunnable(command, id));
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(command);
        }

        static class MonitoredRunnable implements Runnable {
            private final Runnable delegate;
            private final String id;
            private final long runnableInstanceCreatedAt;

            public MonitoredRunnable(Runnable delegate, String id) {
                this.delegate = delegate;
                this.id = id;
                this.runnableInstanceCreatedAt = System.nanoTime();
            }

            @Override
            public void run() {
                log(id + ": Queue wait time took: " + Duration.ofNanos(System.nanoTime() - runnableInstanceCreatedAt).toMillis() + " ms");
                long startExecution = System.nanoTime();
                delegate.run();
                log(id + ": Task execution took: " + Duration.ofNanos(System.nanoTime() - startExecution).toMillis() + " ms");
            }
        }
    }

    private static void runtimeThrowingSleep(int seconds) {
        try {
            // Thread sleep is non-blocking on Virtual Threads
            Thread.sleep(Duration.ofSeconds(seconds));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
