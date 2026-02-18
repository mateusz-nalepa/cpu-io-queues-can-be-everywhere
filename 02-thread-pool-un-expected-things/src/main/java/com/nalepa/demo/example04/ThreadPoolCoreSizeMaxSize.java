package com.nalepa.demo.example04;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolCoreSizeMaxSize {
    public static void main(String[] args) throws Exception {
        threadPoolIsNotCreatingThreadsWhenQueueIsNotEmpty();
        // threadPoolCorePoolSizeAndMaxPoolSizeAreTheSame();
    }

    static void threadPoolIsNotCreatingThreadsWhenQueueIsNotEmpty() throws Exception {
        int corePoolSize = 1;
        int maxPoolSize = 10;
        int queueSize = 20;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                1L,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(queueSize),
                new SimpleThreadFactory("customThread")
        );
        executor.execute(() -> sleepForOneHour(Thread.currentThread().getName() + " : Will sleep for an hour"));
        Thread.sleep(100);
        System.out.println();
        System.out.println("Number of threads in pool: " + executor.getPoolSize() + ", number of tasks in queue: " + executor.getQueue().size());
        if (executor.getPoolSize() != 1) throw new IllegalStateException("Pool size is: " + executor.getPoolSize());
        for (int index = 0; index < queueSize; index++) {
            final int idx = index;
            executor.execute(() -> sleepForOneHour(Thread.currentThread().getName() + " : Index: " + idx + " : Will sleep for an hour"));
        }
        Thread.sleep(100);
        System.out.println();
        System.out.println("Number of threads in pool: " + executor.getPoolSize() + ", number of tasks in queue: " + executor.getQueue().size());
        System.out.println("It would be nice to have 6 threads in pool here XD");
        System.out.println();
        if (executor.getPoolSize() != 1) throw new IllegalStateException("Pool size is: " + executor.getPoolSize());
        for (int index = 0; index < 5; index++) {
            final int idx = index;
            executor.execute(() -> sleepForOneHour(Thread.currentThread().getName() + " : New Index: " + idx + " : Will sleep for an hour"));
        }
        Thread.sleep(100);
        System.out.println();
        System.out.println("Number of threads in pool: " + executor.getPoolSize() + ", number of tasks in queue: " + executor.getQueue().size());
        if (executor.getPoolSize() != 6) throw new IllegalStateException("Pool size is: " + executor.getPoolSize());
        executor.shutdownNow();
    }

    static void threadPoolCorePoolSizeAndMaxPoolSizeAreTheSame() throws Exception {
        int poolSize = 10;
        int queueSize = 20;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                1L,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(queueSize),
                new SimpleThreadFactory("customThread")
        );
        for (int index = 0; index < poolSize; index++) {
            final int idx = index;
            executor.execute(() -> sleepForOneHour(Thread.currentThread().getName() + " : Index: " + idx + " : Will sleep for an hour"));
        }
        Thread.sleep(100);
        System.out.println("Number of threads in pool: " + executor.getPoolSize() + ", number of tasks in queue: " + executor.getQueue().size());
        if (executor.getPoolSize() != poolSize) throw new IllegalStateException("Pool size is: " + executor.getPoolSize());
        executor.shutdownNow();
    }

    static void sleepForOneHour(String message) {
        try {
            System.out.println(message);
            Thread.sleep(Duration.ofHours(1).toMillis());
        } catch (InterruptedException e) {
            // Task interrupted, shutting down gracefully
        }
    }

    static class SimpleThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger counter = new AtomicInteger(0);
        public SimpleThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        @Override
        public Thread newThread(Runnable r) {
            String name = namePrefix + "-" + counter.incrementAndGet();
            return new Thread(r, name);
        }
    }
}
