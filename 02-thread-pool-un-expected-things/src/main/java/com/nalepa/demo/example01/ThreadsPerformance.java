package com.nalepa.demo.example01;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ThreadsPerformance {
    public static void main(String[] args) throws Exception {
        System.out.println();
        System.out.println("printAverageNumberOfIterationsPerThreadsCountWhenQueueIsNotUsed");
        ThreadsPerformanceTest.printAverageNumberOfIterationsPerThreadsCountWhenQueueIsNotUsed();

        System.out.println();
        System.out.println("printAverageNumberOfIterationsPerThreadsCountWhenQueueIsUsed");
        ThreadsPerformanceTest.printAverageNumberOfIterationsPerThreadsCountWhenQueueIsUsed();
    }

    static class ThreadsPerformanceTest {
        static void printAverageNumberOfIterationsPerThreadsCountWhenQueueIsNotUsed() throws Exception {
            // warmup
            executeIterations(1);
            Operations.heavyCpuCode(5);

            int cpu = Runtime.getRuntime().availableProcessors();
            List<Integer> threadsTaskCount = List.of(
                    cpu,
                    cpu * 2,
                    cpu * 4,
                    cpu * 16
            );
            for (int threads : threadsTaskCount) {
                String avg = executeIterations(threads);
                System.out.println("Number of iterations per thread number: " + threads + ": \t " + avg);
            }
        }

        static void printAverageNumberOfIterationsPerThreadsCountWhenQueueIsUsed() throws Exception {
            // warmup
            executeIterations(1);
            Operations.heavyCpuCode(5);

            int threadCount = Runtime.getRuntime().availableProcessors();
            List<Integer> taskCount = List.of(
                    threadCount,
                    threadCount * 2,
                    threadCount * 4,
                    threadCount * 16
            );
            for (int tasks : taskCount) {
                String avg = executeIterations(threadCount, tasks);
                System.out.println("Number of iterations per thread number: " + tasks + ": \t " + avg);
            }
        }

        private static String executeIterations(int threadCount) throws Exception {
            return executeIterations(threadCount, 0);
        }

        private static String executeIterations(int threadCount, int taskCount) throws Exception {
            ExecutorService executor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000));
            List<Future<BigInteger>> futures = new ArrayList<>();
            if (taskCount > 0) {
                for (int i = 0; i < taskCount; i++) {
                    futures.add(executor.submit(() -> Operations.heavyCpuCode(1)));
                }
            } else {
                for (int i = 0; i < threadCount; i++) {
                    futures.add(executor.submit(() -> Operations.heavyCpuCode(1)));
                }
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
            List<BigInteger> results = new ArrayList<>();
            for (Future<BigInteger> f : futures) {
                results.add(f.get());
            }
            BigDecimal sum = BigDecimal.ZERO;
            for (BigInteger value : results) {
                sum = sum.add(new BigDecimal(value));
            }
            BigDecimal avg = sum.divide(new BigDecimal(results.size()), 0, RoundingMode.HALF_UP);
            return formatted(avg);
        }

        private static String formatted(BigDecimal value) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator(' ');
            return new DecimalFormat("#,###", symbols).format(value);
        }
    }

    static class Operations {
        static BigInteger heavyCpuCode(long cpuOperationDelaySeconds) {
            long startTime = System.nanoTime();
            BigInteger iterations = BigInteger.ZERO;
            while (Duration.ofNanos(System.nanoTime() - startTime).getSeconds() < cpuOperationDelaySeconds) {
                Math.sqrt(iterations.doubleValue());
                iterations = iterations.add(BigInteger.ONE);
            }
            return iterations;
        }

        static void someBlockingIO(long blockingTimeSeconds) {
            if (!Thread.currentThread().isVirtual()) {
                try {
                    Thread.sleep(Duration.ofSeconds(blockingTimeSeconds).toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                throw new RuntimeException("Thread.sleep is non-blocking on virtual threads");
            }
        }
    }
}




