package com.nalepa.demo.example04

import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

fun main() {
    threadPoolIsNotCreatingThreadsWhenQueueIsNotEmpty()
//    threadPoolCorePoolSizeAndMaxPoolSizeAreTheSame()
}

fun threadPoolIsNotCreatingThreadsWhenQueueIsNotEmpty() {
    val corePoolSize = 1
    val maxPoolSize = 10
    val queueSize = 20

    val executor =
        ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            1L,
            TimeUnit.MINUTES,
            LinkedBlockingQueue(queueSize),
            SimpleThreadFactory("customThread")
        )

    executor.execute {
        sleepForOneHour("${Thread.currentThread().name} : Will sleep for an hour")
    }

    Thread.sleep(Duration.ofMillis(100))
    println("Number of threads in pool: ${executor.poolSize}, number of tasks in queue: ${executor.queue.size}")
    check(executor.poolSize == 1) { "Pool size is: ${executor.poolSize}" }

    repeat(queueSize) { index ->
        executor.execute {
            sleepForOneHour("${Thread.currentThread().name} : Index: ${index} : Will sleep for an hour")
        }
    }

    Thread.sleep(Duration.ofMillis(100))
    println("Number of threads in pool: ${executor.poolSize}, number of tasks in queue: ${executor.queue.size}")
    println("It would be nice to have 6 threads in pool here XD")
    check(executor.poolSize == 1) { "Pool size is: ${executor.poolSize}" }


    repeat(5) { index ->
        executor.execute {
            sleepForOneHour("${Thread.currentThread().name} : New Index: ${index} : Will sleep for an hour")
        }
    }

    Thread.sleep(Duration.ofMillis(100))
    println("Number of threads in pool: ${executor.poolSize}, number of tasks in queue: ${executor.queue.size}")
    check(executor.poolSize == 6) { "Pool size is: ${executor.poolSize}" }

    executor.shutdownNow()
}

fun threadPoolCorePoolSizeAndMaxPoolSizeAreTheSame() {
    val poolSize = 10
    val queueSize = 20

    val executor =
        ThreadPoolExecutor(
            poolSize,
            poolSize,
            // in this scenario, those values does not matter :D
            1L,
            TimeUnit.MINUTES,
            LinkedBlockingQueue(queueSize),
            SimpleThreadFactory("customThread")
        )


    repeat(poolSize) { index ->
        executor.execute {
            sleepForOneHour("${Thread.currentThread().name} : Index: ${index} : Will sleep for an hour")
        }
    }

    Thread.sleep(Duration.ofMillis(100))
    println("Number of threads in pool: ${executor.poolSize}, number of tasks in queue: ${executor.queue.size}")
    check(executor.poolSize == poolSize) { "Pool size is: ${executor.poolSize}" }

    executor.shutdownNow()
}

fun sleepForOneHour(message: String) {
    try {
        println(message)
        Thread.sleep(Duration.ofHours(1))
    } catch (e: InterruptedException) {
//        println("Task interrupted, shutting down gracefully")
        return
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
