package com.nalepa.demo.common

import java.util.concurrent.Future

fun <T> Future<T>.safeGetOnVirtual(): T {
    if (Thread.currentThread().isVirtual) {
        // carrier-thread returns to Loom pool, so this `get()` from perspective of virtual thread is non-blocking
        return this.get()
    }
    throw RuntimeException("safeGetOnVirtual called from carrier thread")
}
