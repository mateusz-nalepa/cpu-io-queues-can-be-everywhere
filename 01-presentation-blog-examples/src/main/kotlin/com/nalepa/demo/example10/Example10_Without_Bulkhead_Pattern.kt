package com.nalepa.demo.example10

import com.nalepa.demo.example12.log
import java.time.Duration
import java.time.LocalTime
import java.util.*

// fast endpoint is waiting in queue :((((((
fun main() {
    val queue = LinkedList<Runnable>()
    val exampleStart = System.nanoTime()

    repeat(4) { index ->
        queue.add(MonitoredRunnable { simulateSlowEndpoint(index) })
    }

    repeat(4) { index ->
        queue.add(MonitoredRunnable { simulateFastEndpoint(index) })
    }

    val workerThread =
        Thread.ofPlatform()
            .name("worker-thread")
            .start {
                while (!queue.isEmpty()) {
                    val runnable = queue.poll()
                    runnable.run()
                }
            }

    workerThread.join()
    log("Example ended after: ${Duration.ofNanos(System.nanoTime() - exampleStart).toSeconds()} s")
}

fun simulateSlowEndpoint(index: Int) {
    log("Start slow for: $index")
    Thread.sleep(Duration.ofSeconds(5))
    log("End slow for: $index")
}

fun simulateFastEndpoint(index: Int) {
    log("Start fast for: $index")
    log("End fast for: $index")
}

fun log(message: String) {
    println("${LocalTime.now()} : ${Thread.currentThread().name} : $message")
}

class MonitoredRunnable(
    private val delegate: Runnable,
) : Runnable {

    val runnableInstanceCreatedAt = System.nanoTime()

    override fun run() {
        log("Queue wait time took: ${Duration.ofNanos(System.nanoTime() - runnableInstanceCreatedAt).toSeconds()} s")

        val startExecution = System.nanoTime()
        delegate.run()
        log("Task execution took: ${Duration.ofNanos(System.nanoTime() - startExecution).toSeconds()} s")
    }
}




