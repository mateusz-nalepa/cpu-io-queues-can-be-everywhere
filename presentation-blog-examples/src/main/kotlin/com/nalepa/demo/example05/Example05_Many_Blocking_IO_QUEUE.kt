package com.nalepa.demo.example05

import java.time.LocalTime
import java.util.*

fun main() {
    val queue = LinkedList<Runnable>()

    // yup, we can write: queue.add { simulateBlockingIO(1) } but Runnable is intentional :P
    repeat(4) { index ->
        queue.add(Runnable { simulateBlockingIO(index) })
    }

    while (!queue.isEmpty()) {
        queue.poll().run()
    }
}

fun simulateBlockingIO(index: Int) {
    println("")
    log("Start blocking IO code for: $index")
    Thread.sleep(5000) // http call, database call, reading from file, whatever what needs external data
    log("End blocking IO code for: $index")
}

fun log(message: String) {
    println("${Thread.currentThread().name} : ${LocalTime.now()} : $message")
}

