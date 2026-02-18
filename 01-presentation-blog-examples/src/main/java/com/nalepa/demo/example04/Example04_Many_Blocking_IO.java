package com.nalepa.demo.example04;

import java.time.LocalTime;

public class Example04_Many_Blocking_IO {
    public static void main(String[] args) {
        simulateBlockingIO(1);
        simulateBlockingIO(2);
        simulateBlockingIO(3);
    }

    static void simulateBlockingIO(int index) {
        System.out.println();
        log("Start blocking IO code for: " + index);
        nonThrowingSleep(5000);
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
