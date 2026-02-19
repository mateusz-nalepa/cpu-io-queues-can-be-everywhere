package com.nalepa.demo.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public final class AsyncUtils {

    private AsyncUtils() {}

    public static <T> T nonBlockingGet(Future<T> future) {
        if (!Thread.currentThread().isVirtual()) {
            throw new RuntimeException("nonBlockingGet called from carrier thread");
        }

        // carrier-thread returns to Loom pool, so this `get()` from perspective of virtual thread is non-blocking
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> CompletableFuture<T> async(ExecutorService on, Supplier<T> lambda) {
        return CompletableFuture.supplyAsync(lambda, on);
    }
}