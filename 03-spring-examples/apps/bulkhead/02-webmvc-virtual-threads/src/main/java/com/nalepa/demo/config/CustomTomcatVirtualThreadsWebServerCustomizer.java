package com.nalepa.demo.config;

import com.nalepa.demo.common.monitored.ExecutorsFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.thread.Threading;
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
@ConditionalOnThreading(Threading.VIRTUAL)
public class CustomTomcatVirtualThreadsWebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> {

    private final ExecutorsFactory executorsFactory;

    public CustomTomcatVirtualThreadsWebServerCustomizer(ExecutorsFactory executorsFactory) {
        this.executorsFactory = executorsFactory;
    }

    @Override
    public void customize(ConfigurableTomcatWebServerFactory factory) {
        factory.addProtocolHandlerCustomizers(protocolHandler -> {

                    // TomcatVirtualThreadsWebServerFactoryCustomizer is executed before this bean
                    var currentExecutor = (ExecutorService) protocolHandler.getExecutor();

                    // There will be given log in logs:
                    // i.m.c.i.b.jvm.ExecutorServiceMetrics     : Failed to bind as org.apache.tomcat.util.threads.VirtualThreadExecutor is unsupported.
                    // but Timer metrics will be available, so we can monitor how long requests are pending before being executed
                    // in order to learn more, have a look at the source code of ExecutorServiceMetrics
                    var monitoredExecutor =
                            executorsFactory.monitorExecutorService(
                                    currentExecutor,
                                    "custom.http.server.pending"
                            );

                    protocolHandler.setExecutor(monitoredExecutor);
                }
        );
    }
}