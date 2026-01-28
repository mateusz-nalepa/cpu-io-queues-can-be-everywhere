package com.nalepa.demo.example9

import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt

// look on Visual VM CPU Usage
fun main() {
    // theads pool number equal to cpu, so it's 100% cpu usage on VisualVM
    val executor =
        Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            SimpleThreadFactory(namePrefix = "workerThread")
        )
    val futures = mutableListOf<Future<*>>()

    repeat(10000) { index ->
        futures.add(
            executor.submit(MonitoredRunnable { simulateBlockingIO(index) })
//            executor.submit(MonitoredRunnable { simulateCpuCode(index) })
        )
    }

    futures.forEach { it.get() }
}

fun simulateBlockingIO(index: Int) {
    log("Start blocking IO code for: $index")
    Thread.sleep(Duration.ofSeconds(5)) // http call, database call, reading from file, whatever what needs external data
    log("End blocking IO code for: $index")
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
        log("Queue wait time took: ${Duration.ofNanos(System.nanoTime() - runnableInstanceCreatedAt).toSeconds()} s")

        val startExecution = System.nanoTime()
        delegate.run()
        log("Task execution took: ${Duration.ofNanos(System.nanoTime() - startExecution).toSeconds()} s")
    }
}


class SimpleThreadFactory(
    private val namePrefix: String
) : ThreadFactory {

    private val counter = AtomicInteger(0)

    override fun newThread(r: Runnable): Thread {
        val name = "$namePrefix-${counter.incrementAndGet()}"
        return Thread(r, name)
    }
}
