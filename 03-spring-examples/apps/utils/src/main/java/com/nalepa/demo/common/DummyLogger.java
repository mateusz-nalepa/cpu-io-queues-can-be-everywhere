package com.nalepa.demo.common;

public final class DummyLogger {

    private DummyLogger() {}

    public static void log(Object caller, String message) {
//        System.out.println(LocalTime.now() + " : " + caller.getClass().getSimpleName() + " : " + Thread.currentThread().getName() + "\n\t\t" + message);
    }
}