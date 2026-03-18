package com.nalepa.demo.common;

import java.math.BigInteger;
import java.time.Duration;

public final class Operations {

    private Operations() {}

    /**
     * condition is based on time of execution, not number of iteration
     */
    public static BigInteger heavyCpuCode(long cpuOperationDelaySeconds) {
        long startTime = System.nanoTime();
        BigInteger iterations = BigInteger.ZERO;
        while (Duration.ofNanos(System.nanoTime() - startTime).getSeconds() < cpuOperationDelaySeconds) {
            Math.sqrt(iterations.doubleValue());
            iterations = iterations.add(BigInteger.ONE);
        }
        return iterations;
    }

    /**
     * simulate blocking I/O
     */
    public static void someBlockingIO(long blockingTimeSeconds)  {
        if (!Thread.currentThread().isVirtual()) {
            try {
                Thread.sleep(Duration.ofSeconds(blockingTimeSeconds));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Thread.sleep is non-blocking on virtual threads");
        }
    }
}