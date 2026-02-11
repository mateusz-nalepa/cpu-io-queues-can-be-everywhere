package com.nalepa.demo.example06

import java.time.Duration
import java.time.LocalTime
import java.util.*

fun main() {
    val queue = LinkedList<Runnable>()

    val monitoredRunnable1 = MonitoredRunnable { simulateBlockingIO(1) }
    val monitoredRunnable2 = MonitoredRunnable { simulateBlockingIO(2) }
    val monitoredRunnable3 = MonitoredRunnable { simulateBlockingIO(3) }
    val monitoredRunnable4 = MonitoredRunnable { simulateBlockingIO(4) }

    queue.add(monitoredRunnable1)
    queue.add(monitoredRunnable2)
    queue.add(monitoredRunnable3)
    queue.add(monitoredRunnable4)

    while (!queue.isEmpty()) {
        val runnable = queue.poll()
        runnable.run()
    }
}

fun simulateBlockingIO(index: Int) {
    log("Start blocking IO code for: $index")
    Thread.sleep(5000) // http call, database call, reading from file, whatever what needs external data
    log("End blocking IO code for: $index")
}

fun log(message: String) {
    println("${Thread.currentThread().name} : ${LocalTime.now()} : $message")
}

class MonitoredRunnable(
    private val delegate: Runnable,
) : Runnable {

    val runnableInstanceCreatedAt = System.nanoTime()

    override fun run() {
        println("")
        log("Queue wait time took: ${Duration.ofNanos(System.nanoTime() - runnableInstanceCreatedAt).toSeconds()} s")

        val startExecution = System.nanoTime()
        delegate.run()
        log("Task execution took: ${Duration.ofNanos(System.nanoTime() - startExecution).toSeconds()} s")
    }
}

