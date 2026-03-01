package com.nalepa.demo.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DummyPrometheusConfig {

    private final PrometheusMeterRegistry meterRegistry;

    public DummyPrometheusConfig(PrometheusMeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // TODO: use MeterRegistryCustomizer<MeterRegistry> beans instead of @PostConstruct
    @PostConstruct
    public void configurePercentiles() {
        meterRegistry
                .config()
                .meterFilter(new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                        return DistributionStatisticConfig
                                .builder()
                                .percentiles(0.99, 0.999)
                                .build()
                                .merge(config);
                    }
                });

        meterRegistry
                .config()
                .meterFilter(
                        MeterFilter.deny(id ->
                                id.getName().equals("http.server.requests")
                                        && id.getTag("uri").equals("/actuator/prometheus")
                        )
                );
    }
}