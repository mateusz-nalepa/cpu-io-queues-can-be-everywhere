package com.nalepa.demo.example13

import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue

// With Staged Event-Driven Architecture
// While there is JSON parsing for index 1 on thread cpu, then we already got data for index 2 on thread io <3
/**
 * In this example there is:
 *  IO Pool -> CPU Pool
 *
 * But it's even more than OK to add more pools, when data from one client depends on data from another client:
 *  IO Pool 1 -> CPU Pool 1 -> IO Pool 2 -> CPU Pool2 etc.
 *
 * We can add just more threads, but then Blocking Factor is a magic.
 * Blocking Factor? Ratio between Blocking Time vs Total Time
 * SharedPool: 3s for I/O, 3s for CPU >= 3s io / 3s io + 3s cpu = Blocking Factor: 0.5
 * SharedPool: 9s for I/O, 3s for CPU >= 9s io / 9s io + 3s cpu = Blocking Factor: 0.75
 *
 * But with SEDA pattern, Blocking Factor is always
 * IO Pool: Blocking Factor ≈ 1
 * CPU Pool: Blocking Factor ≈ 0
 * So no more calculating what's the thread pool size!
 *
 * For IO: Threads Number = As many as needed, we don't want here tasks which are waiting in a queue
 * For CPU: Threads Number = CPU Number with some queue Size
 *
 * Task#1:
 *   I/O(1)---3s---
 *                 CPU(1)-----5s-----
 *
 * Task#2:                      (added here at t=5s)
 *                              I/O(2)---3s--- - no waiting in queue, case io thread was ready to sent data and block while waiting for them
 *                                            CPU(2)-----5s-----
 */
fun main() {
    val queueForIo = LinkedBlockingQueue<Runnable>()
    val countDownLatchForIO = CountDownLatch(2)

    val queueForCPU = LinkedBlockingQueue<Runnable>()
    val countDownLatchForCPU = CountDownLatch(2)

    val exampleStart = System.nanoTime()

    Thread.ofPlatform()
        .name("io-thread")
        .start {
            while (true) {
                queueForIo.take().run() // take instead of pull, here it's blocking waiting
                countDownLatchForIO.countDown()

                if (countDownLatchForIO.count == 0L) {
                    break
                }
            }
        }

    Thread.ofPlatform()
        .name("cpu-thread")
        .start {
            while (true) {
                queueForCPU.take().run() // take instead of pull, here it's blocking waiting
                countDownLatchForCPU.countDown()

                if (countDownLatchForCPU.count == 0L) {
                    break
                }
            }
        }

    val startTimeForIndex1 = System.nanoTime()
    queueForIo.add(MonitoredRunnable(1) {
        val byteArrayFromClient = ioHttpClientCall(1)

        // io thread part is done, now it's time for CPU
        queueForCPU.add(MonitoredRunnable(1) {
            val parsedData = cpuJsonParsing(byteArrayFromClient)
            log(
                "######## Data ready for index 1. It took: " +
                        "${Duration.ofNanos(System.nanoTime() - startTimeForIndex1).toSeconds()} s"
            )
        })
    })


    // in order to make sure, that worker-thread is parsing data for index 1
    Thread.sleep(Duration.ofSeconds(5))

    val startTimeForIndex2 = System.nanoTime()
    queueForIo.add(MonitoredRunnable(2) {
        val byteArrayFromClient = ioHttpClientCall(2)

        // io thread part is done, now it's time for CPU
        queueForCPU.add(MonitoredRunnable(2) {
            val parsedData = cpuJsonParsing(byteArrayFromClient)
            log(
                "######## Data ready for index 2. It took: " +
                        "${Duration.ofNanos(System.nanoTime() - startTimeForIndex2).toSeconds()} s"
            )
        })
    })


    countDownLatchForIO.await()
    countDownLatchForCPU.await()
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
