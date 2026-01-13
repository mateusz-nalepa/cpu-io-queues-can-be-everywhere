package com.nalepa.demo.common


import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.concurrent.*

class OperationsPerformanceTest {

    /**
     * more threads, less average number of iterations
     */
    @Test
    fun printAverageNumberOfIterationsPerThreadsCountWhenWeAreNotUsingQueue() {
        // warmup
        executeIterations(1)
        Operations.heavyCpuCode(5)

        // test
        val threadsTaskCount = // threads count == task count, so every thread is executing task
            listOf(
                Runtime.getRuntime().availableProcessors(),
                1,
                2,
                4,
                20,
                50,
                100,
                200,
                1000,
            )

        threadsTaskCount
            .map { it to executeIterations(it) }
            .forEach { println("Number of iterations per thread number: ${it.first}: \t ${it.second}") }
    }

    /**
     * more tasks - they are just waiting in a queue
     */
    @Test
    fun printAverageNumberOfIterationsPerThreadsCountWhenWeAreUsingQueue() {
        // warmup
        executeIterations(1)
        Operations.heavyCpuCode(5)

        // test
        val threadCount = Runtime.getRuntime().availableProcessors()
        val taskCount =
            listOf(
                threadCount,
                threadCount * 2,
                threadCount * 4,
                threadCount * 10,
            )

        taskCount
            .map { it to executeIterations(threadCount, it) }
            .forEach { println("Number of iterations per thread number: ${it.first}: \t ${it.second}") }
    }

    private fun executeIterations(threadCount: Int, taskCount: Int = 0): String {
        val executor = ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, LinkedBlockingQueue(10000))
        val futures = mutableListOf<Future<BigInteger>>()

        if (taskCount > 0) {
            repeat(taskCount) {
                futures += executor.submit(Callable {
                    Operations.heavyCpuCode(1)
                })
            }
        } else {
            repeat(threadCount) {
                futures += executor.submit(Callable {
                    Operations.heavyCpuCode(1)
                })
            }
        }


        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)

        val results = futures.map { it.get() }

        val sum = results.fold(BigDecimal.ZERO) { acc, value -> acc + value.toBigDecimal() }
        val avg: BigDecimal = sum.divide(BigDecimal(results.size), 0, RoundingMode.HALF_UP)

        return avg.formatted()
    }
}

fun BigDecimal.formatted(): String {
    val symbols = DecimalFormatSymbols().apply {
        groupingSeparator = ' '
    }
    return DecimalFormat("#,###", symbols).format(this)
}
