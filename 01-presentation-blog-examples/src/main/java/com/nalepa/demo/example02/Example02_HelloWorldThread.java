package com.nalepa.demo.example02;

// open VisualVM and show threads present
public class Example02_HelloWorldThread {
    public static void main(String[] args) {
        while (true) {
            System.out.println("Hello World from: " + Thread.currentThread().getName() + "!");
        }
    }
}
