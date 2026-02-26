package com.nalepa.demo.config

import com.nalepa.demo.common.monitored.ExecutorsFactory
import org.apache.coyote.ProtocolHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading
import org.springframework.boot.thread.Threading
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService

@Component
@ConditionalOnThreading(Threading.VIRTUAL)
class CustomTomcatVirtualThreadsWebServerCustomizer(
    private val executorsFactory: ExecutorsFactory,
) : WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> {

    override fun customize(factory: ConfigurableTomcatWebServerFactory) {
        factory.addProtocolHandlerCustomizers(
            TomcatProtocolHandlerCustomizer { protocolHandler: ProtocolHandler ->

                // TomcatVirtualThreadsWebServerFactoryCustomizer is executed before this bean
                val currentExecutor = protocolHandler.executor as ExecutorService

                // There will be given log in logs:
                // i.m.c.i.b.jvm.ExecutorServiceMetrics     : Failed to bind as org.apache.tomcat.util.threads.VirtualThreadExecutor is unsupported.
                // but Timer metrics will be available, so we can monitor how long requests are pending before being executed
                // in order to learn more, have a look at the source code of ExecutorServiceMetrics
                val monitoredExecutor =
                    executorsFactory.monitorExecutorService(
                        currentExecutor,
                        "custom.http.server.pending"
                    )

                protocolHandler.executor = monitoredExecutor
            })
    }

}