package com.nalepa.demo.common;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DummyPrometheusConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryPercentilesCustomizer() {
        return meterRegistry -> meterRegistry
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
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryPrometheusFilterCustomizer() {
        return meterRegistry -> meterRegistry
                .config()
                .meterFilter(
                        MeterFilter.deny(id ->
                                id.getName().equals("http.server.requests")
                                        && id.getTag("uri").equals("/actuator/prometheus")
                        )
                );
    }
}