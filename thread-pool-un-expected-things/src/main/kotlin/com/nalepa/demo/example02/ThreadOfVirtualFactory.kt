package com.nalepa.demo.example02

import java.util.concurrent.Executors

fun main() {
    virtualThreadFactoryThreadsAreScheduledOnForkJoinPool()
}

fun virtualThreadFactoryThreadsAreScheduledOnForkJoinPool() {
    val executor1 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("one").factory())
    val executor2 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("two").factory())
    val executor3 = Executors.newFixedThreadPool(1, Thread.ofVirtual().name("three").factory())

    // CurrentThread: VirtualThread[#37,one]/runnable@ForkJoinPool-1-worker-1
    val future1 = executor1.submit { println("CurrentThread: " + Thread.currentThread()) }

    // CurrentThread: VirtualThread[#39,two]/runnable@ForkJoinPool-1-worker-2
    val future2 = executor2.submit { println("CurrentThread: " + Thread.currentThread()) }

    // CurrentThread: VirtualThread[#41,three]/runnable@ForkJoinPool-1-worker-3
    val future3 = executor3.submit { println("CurrentThread: " + Thread.currentThread()) }

    future1.get()
    future2.get()
    future3.get()
}





