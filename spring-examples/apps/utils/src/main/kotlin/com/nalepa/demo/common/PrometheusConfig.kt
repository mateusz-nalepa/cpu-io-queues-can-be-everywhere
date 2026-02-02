package com.nalepa.demo.common

import io.micrometer.core.instrument.Meter.Id
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component


@Component
class DummyPrometheusConfig {

    @Bean
    fun meterRegistryPercentilesCustomizer(): MeterRegistryCustomizer<MeterRegistry> =
        MeterRegistryCustomizer { meterRegistry ->
            meterRegistry
                .config()
                .meterFilter(object : MeterFilter {
                    override fun configure(id: Id, config: DistributionStatisticConfig): DistributionStatisticConfig? {
                        return DistributionStatisticConfig
                            .builder()
                            .percentiles(0.99, 0.999)
                            .build()
                            .merge(config)
                    }
                })
        }

    @Bean
    fun meterRegistryPrometheusFilterCustomizer(): MeterRegistryCustomizer<MeterRegistry> =
        MeterRegistryCustomizer { meterRegistry ->
            meterRegistry
                .config()
                .meterFilter(
                    MeterFilter.deny { id ->
                        id.name == "http.server.requests"
                                && id.getTag("uri") == "/actuator/prometheus"
                    }
                )
        }

}