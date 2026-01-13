package com.nalepa.demo.common

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CustomObservationHandler(
    private val meterRegistry: MeterRegistry,
) : ObservationHandler<Observation.Context> {
    override fun supportsContext(context: Observation.Context): Boolean {
        return true
    }

    override fun onStart(context: Observation.Context) {

        if (context.name == "http.client.requests") {
            DummyLogger.log(this, "START measuring client")
        }

        if (context.name == "http.server.requests") {
            DummyLogger.log(this, "START measuring server")
        }
    }

    override fun onStop(context: Observation.Context) {
        val sample = context.getRequired<Timer.Sample>(Timer.Sample::class.java)
        val time = sample.stop(Timer.builder("XD").register(meterRegistry))

        if (context.name == "http.client.requests") {
            DummyLogger.log(this, "END measuring client. Took: ${Duration.ofNanos(time)}")
        }

        if (context.name == "http.server.requests") {
            // NOTE: learn difference between low and high cardinality
            val endpoint = context.highCardinalityKeyValues.sorted().find { it.key == "http.url" }?.value

            if (endpoint != "/actuator/prometheus") {
                DummyLogger.log(this, "END measuring: $endpoint server took: ${Duration.ofNanos(time)}")
            }

        }
    }
}