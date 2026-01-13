package com.nalepa.demo.common

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.function.Supplier

fun <T> Future<T>.safeGetOnVirtual(): T {
    if (Thread.currentThread().isVirtual) {
        // carrier-thread returns to Loom pool, so this `get()` from perspective of virtual thread is non-blocking
        return this.get()
    }
    throw RuntimeException("safeGetOnVirtual called from carrier thread")
}

fun <T> async(on: ExecutorService, lambda: Supplier<T>): CompletableFuture<T> {
    return CompletableFuture.supplyAsync(lambda, on)
}