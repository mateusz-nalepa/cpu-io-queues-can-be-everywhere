package com.nalepa.demo.example12

import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue

// Without Staged Event-Driven Architecture
// While there is JSON parsing for index 1, we could already trigger I/O for index 2... but we are not doing it :(

/**
 * In this example there is only One Pool which is doing both I/O and CPU
 *
 * Task#1:
 *   I/O(1)---3s---
 *                 CPU(1)-----5s-----
 *
 * Task#2:                      (added here at t=5s)
 *                              queue---3s---
 *                                   I/O(2)---3s---
 *                                                 CPU(2)-----5s-----
 */

fun main() {
    val queue = LinkedBlockingQueue<Runnable>()
    val countDownLatch = CountDownLatch(2)

    val exampleStart = System.nanoTime()

    Thread.ofPlatform()
        .name("worker-thread")
        .start {
            while (true) {
                queue.take().run() // take instead of pull, here it's blocking waiting
                countDownLatch.countDown()

                if (countDownLatch.count == 0L) {
                    break
                }
            }
        }

    val startTimeForIndex1 = System.nanoTime()
    queue.add(MonitoredRunnable(1) {
        val responseFromFirstClient = ioHttpClientCall(1)
        val responseFromSecondClient = cpuJsonParsing(responseFromFirstClient)
        log(
            "######## Data ready for index 1. It took: " +
                    "${Duration.ofNanos(System.nanoTime() - startTimeForIndex1).toSeconds()} s"
        )
    })


    // in order to make sure, that worker-thread is parsing data for index 1
    Thread.sleep(Duration.ofSeconds(5))

    val startTimeForIndex2 = System.nanoTime()
    queue.add(MonitoredRunnable(2) {
        val responseFromFirstClient = ioHttpClientCall(2)
        val responseFromSecondClient = cpuJsonParsing(responseFromFirstClient)
        log(
            "######## Data ready for index 2. It took: " +
                    "${Duration.ofNanos(System.nanoTime() - startTimeForIndex2).toSeconds()} s"
        )
    })

    countDownLatch.await()
    log("Example ended after: ${Duration.ofNanos(System.nanoTime() - exampleStart).toSeconds()} s")
}

fun ioHttpClientCall(index: Int): ByteArray {
    // get ByteArray from HttpClient
    log("Start getting data for index: $index")
    Thread.sleep(Duration.ofSeconds(3))
    log("Data ready for index: $index")
    return "data with index: $index".toByteArray()
}

fun cpuJsonParsing(byteArray: ByteArray) {
    // parse ByteArray to some Object, e.q. objectMapper.readValue(bytes, User::class.java)
    log("Start parsing data for ${byteArray.toString(Charsets.UTF_8)}")
    Thread.sleep(Duration.ofSeconds(5))
    log("End parsing data for ${byteArray.toString(Charsets.UTF_8)}")
}

fun log(message: String) {
    println("${LocalTime.now()} : ${Thread.currentThread().name} : $message")
}

class MonitoredRunnable(
    private val index: Int, // this example is a little bit harder, that's why index is added here
    private val delegate: Runnable,
) : Runnable {

    val runnableInstanceCreatedAt = System.nanoTime()

    override fun run() {
        log(
            "Index: $index. Queue wait time took: " +
                    "${Duration.ofNanos(System.nanoTime() - runnableInstanceCreatedAt).toSeconds()} s"
        )

        val startExecution = System.nanoTime()
        delegate.run()
        log(
            "Index: $index. Task execution took: " +
                    "${Duration.ofNanos(System.nanoTime() - startExecution).toSeconds()} s"
        )
    }
}


