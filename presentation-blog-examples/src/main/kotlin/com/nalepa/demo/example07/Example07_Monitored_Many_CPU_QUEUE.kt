package com.nalepa.demo.example07

import java.time.Duration
import java.time.LocalTime
import java.util.*
import kotlin.math.sqrt

fun main() {
    val queue = LinkedList<Runnable>()

    // yup, we can write: queue.add { simulateBlockingIO(1) } but Runnable is intentional :P
    repeat(4) { index ->
        queue.add(MonitoredRunnable { simulateCpuCode(index) })
    }

    while (!queue.isEmpty()) {
        val runnable = queue.poll()
        runnable.run()
    }
}

fun simulateCpuCode(index: Int): Long {
    log("Start CPU code for: $index")
    val startTime = System.nanoTime()
    var iteration = 0L
    while (Duration.ofNanos(System.nanoTime() - startTime).seconds < 5) {
        // whatever cpu is doing, compression, JSON serialization, deserialization, etc
        iteration++
        sqrt(iteration.toDouble())
    }
    log("End CPU code for: $index")
    return iteration
}

fun log(message: String) {
    println("${Thread.currentThread().name} : ${LocalTime.now()} : $message")
}

class MonitoredRunnable(
    private val delegate: Runnable,
) : Runnable {

    val runnableInstanceCreatedAt = System.nanoTime()

    init {
        log("Created MonitoredRunnable instance")
    }

    override fun run() {
        println("")
        log("Queue wait time took: ${Duration.ofNanos(System.nanoTime() - runnableInstanceCreatedAt).toSeconds()} s")

        val startExecution = System.nanoTime()
        delegate.run()
        log("Task execution took: ${Duration.ofNanos(System.nanoTime() - startExecution).toSeconds()} s")
    }
}

