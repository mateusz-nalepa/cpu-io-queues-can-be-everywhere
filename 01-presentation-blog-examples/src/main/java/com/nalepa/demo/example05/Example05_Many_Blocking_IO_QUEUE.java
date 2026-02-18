package com.nalepa.demo.example05;

import java.time.LocalTime;
import java.util.LinkedList;

public class Example05_Many_Blocking_IO_QUEUE {
    public static void main(String[] args) {
        LinkedList<Runnable> queue = new LinkedList<>();

        Runnable runnable1 = () -> simulateBlockingIO(1);
        Runnable runnable2 = () -> simulateBlockingIO(2);
        Runnable runnable3 = () -> simulateBlockingIO(3);

        queue.add(runnable1);
        queue.add(runnable2);
        queue.add(runnable3);

        while (!queue.isEmpty()) {
            Runnable runnable = queue.poll();
            runnable.run();
        }
    }

    static void simulateBlockingIO(int index) {
        System.out.println();
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
}
