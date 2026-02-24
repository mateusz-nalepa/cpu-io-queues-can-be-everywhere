package com.nalepa.demo.example02;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadOfVirtualFactory {
    public static void main(String[] args) throws Exception {
        // That's for Java 24, maybe in the future, there will be some changes in that area.
        virtualThreadFactoryThreadsAreScheduledOnForkJoinPool();
    }

    static void virtualThreadFactoryThreadsAreScheduledOnForkJoinPool() throws Exception {
        var executor1 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("one").factory());
        var executor2 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("two").factory());
        var executor3 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("three").factory());

        Future<?> future1 = executor1.submit(() -> System.out.println("CurrentThread: " + Thread.currentThread()));
        Future<?> future2 = executor2.submit(() -> System.out.println("CurrentThread: " + Thread.currentThread()));
        Future<?> future3 = executor3.submit(() -> System.out.println("CurrentThread: " + Thread.currentThread()));

        future1.get();
        future2.get();
        future3.get();
    }
}
