package com.nalepa.demo.example03;

import java.time.LocalTime;

public class Example03_Blocking_IO {
    public static void main(String[] args) {
        simulateBlockingIO();
    }

    static void simulateBlockingIO() {
        log("Start blocking IO code");
        // http call, database call, reading from file, whatever what needs external data
        nonThrowingSleep(5000);
        log("End blocking IO code");
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
