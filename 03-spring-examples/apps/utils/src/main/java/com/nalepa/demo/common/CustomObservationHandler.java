package com.nalepa.demo.common;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CustomObservationHandler implements ObservationHandler<Observation.Context> {

    private final MeterRegistry meterRegistry;

    public CustomObservationHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean supportsContext(Observation.@NonNull Context context) {
        return true;
    }

    @Override
    public void onStart(Observation.Context context) {
        if ("http.client.requests".equals(context.getName())) {
            DummyLogger.log(this, "START measuring client");
        }

        if ("http.server.requests".equals(context.getName())) {
            DummyLogger.log(this, "START measuring server");
        }
    }

    @Override
    public void onStop(Observation.Context context) {
        Timer.Sample sample = context.getRequired(Timer.Sample.class);
        long time = sample.stop(Timer.builder("XD").register(meterRegistry));

        if ("http.client.requests".equals(context.getName())) {
            DummyLogger.log(this, "END measuring client. Took: " + Duration.ofNanos(time));
        }

        if ("http.server.requests".equals(context.getName())) {
            // NOTE: learn difference between low and high cardinality
            String endpoint = context.getHighCardinalityKeyValues().stream()
                    .sorted()
                    .filter(kv -> "http.url".equals(kv.getKey()))
                    .map(kv -> kv.getValue())
                    .findFirst()
                    .orElse(null);

            if (!"/actuator/prometheus".equals(endpoint)) {
                DummyLogger.log(this, "END measuring: " + endpoint + " server took: " + Duration.ofNanos(time));
            }
        }
    }
}