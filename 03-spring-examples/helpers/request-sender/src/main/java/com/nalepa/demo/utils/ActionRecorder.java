package com.nalepa.demo.utils;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ActionRecorder {

    private final MeterRegistry meterRegistry;

    public ActionRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String type, Runnable action) {
        long start = System.nanoTime();

        action.run();

        if (type.equals("warmup")) {
            // warmup not needed to be visible in grafana
            return;
        }

        Duration duration = Duration.ofNanos(System.nanoTime() - start);
        meterRegistry
                .timer("custom.http.request", "type", type)
                .record(duration);
    }
}
