package com.nalepa.demo.utils;

import java.time.LocalTime;

public class RequestSenderLogger {
    public static void log(Object caller, String message) {
        System.out.println(LocalTime.now() + " : " + caller.getClass().getSimpleName() + " : " + Thread.currentThread().getName() + " ### " + message);
    }
}