package com.nalepa.demo.common

import java.math.BigInteger
import java.time.Duration
import kotlin.math.sqrt

object Operations {

    /**
     * condition is based on time of execution, not number of iteration
     */
    fun heavyCpuCode(cpuOperationDelaySeconds: Long): BigInteger {
        val startTime = System.nanoTime()
        var iterations = BigInteger.ZERO
        while (Duration.ofNanos(System.nanoTime() - startTime).seconds < cpuOperationDelaySeconds) {
            sqrt(iterations.toDouble())
            iterations++
        }
        return iterations
    }

    /**
     * simulate blocking I/O
     */
    fun someBlockingIO(blockingTimeSeconds: Long) {
        // simulate blocking I/O
        if (!Thread.currentThread().isVirtual) {
            Thread.sleep(Duration.ofSeconds(blockingTimeSeconds))
        } else {
            throw RuntimeException("Thread.sleep is non-blocking on virtual threads")
        }
    }

}