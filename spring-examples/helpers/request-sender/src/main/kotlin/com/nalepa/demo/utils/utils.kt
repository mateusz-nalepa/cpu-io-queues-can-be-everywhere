package com.nalepa.demo.utils

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalTime

const val SIMULATION_STARTED_TEXT =
    "Simulation stared. Remember to restart apps before running another command, just in case"

object RequestSenderLogger {
    fun log(caller: Any, message: String) {
        println("${LocalTime.now()} : ${caller.javaClass.simpleName} : ${Thread.currentThread().name} ### $message")
    }
}

@Component
class ActionRecorder(
    private val meterRegistry: MeterRegistry,
) {

    fun record(type: String, action: () -> Unit) {
        val start = System.nanoTime()

        action()
        val duration = Duration.ofNanos(System.nanoTime() - start)
        meterRegistry
            .timer("custom.http.request", "type", type)
            .record(duration)
    }

}

interface RequestSenderConfig {
    fun numberOfRequestInBatch(): Int
}

object CpuCountRequestConfig : RequestSenderConfig {
    override fun numberOfRequestInBatch(): Int = Runtime.getRuntime().availableProcessors()
}

class UserProvidedRequestConfig(val count: Int) : RequestSenderConfig {
    override fun numberOfRequestInBatch(): Int = count
}
