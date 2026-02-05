package com.nalepa.demo.example11

import java.time.Duration
import java.time.LocalTime
import java.util.*

// fast endpoints are really fast! <3
fun main() {
    val queueForSlow = LinkedList<Runnable>()
    val queueForFast = LinkedList<Runnable>()

    repeat(4) { index ->
        queueForSlow.add(MonitoredRunnable { simulateSlowEndpoint(index) })
    }

    repeat(4) { index ->
        queueForFast.add(MonitoredRunnable { simulateFastEndpoint(index) })
    }

    val threadForSlow =
        Thread.ofPlatform()
            .name("thread-for-slow")
            .start {
                while (!queueForSlow.isEmpty()) {
                    val runnable = queueForSlow.poll()
                    runnable.run()
                }
            }

    val threadForFast =
        Thread.ofPlatform()
            .name("thread-for-fast")
            .start {
                while (!queueForFast.isEmpty()) {
                    val runnable = queueForFast.poll()
                    runnable.run()
                }
                log("#### Fast tasks finished! ####")
            }

    threadForSlow.join()
    threadForFast.join()

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


