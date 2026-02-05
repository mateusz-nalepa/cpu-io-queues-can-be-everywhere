package com.nalepa.demo.example05

import java.time.LocalTime
import java.util.*

fun main() {
    val queue = LinkedList<Runnable>()

    val runnable1 = Runnable { simulateBlockingIO(1) }
    val runnable2 = Runnable { simulateBlockingIO(2) }
    val runnable3 = Runnable { simulateBlockingIO(3) }
    val runnable4 = Runnable { simulateBlockingIO(4) }

    queue.add(runnable1)
    queue.add(runnable2)
    queue.add(runnable3)
    queue.add(runnable4)

    while (!queue.isEmpty()) {
        val runnable = queue.poll()
        runnable.run()
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

