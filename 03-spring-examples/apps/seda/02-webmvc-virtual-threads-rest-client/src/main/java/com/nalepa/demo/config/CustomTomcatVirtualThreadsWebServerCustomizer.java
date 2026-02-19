package com.nalepa.demo.config;

import com.nalepa.demo.common.monitored.ExecutorsFactory;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.thread.Threading;
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnThreading(Threading.VIRTUAL)
public class CustomTomcatVirtualThreadsWebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> {

    private final ExecutorsFactory executorsFactory;

    public CustomTomcatVirtualThreadsWebServerCustomizer(ExecutorsFactory executorsFactory) {
        this.executorsFactory = executorsFactory;
    }

    // modified code from: TomcatVirtualThreadsWebServerFactoryCustomizer
    @Override
    public void customize(ConfigurableTomcatWebServerFactory factory) {
        factory.addProtocolHandlerCustomizers(
                (TomcatProtocolHandlerCustomizer<ProtocolHandler>) protocolHandler ->
                        protocolHandler.setExecutor(
                                executorsFactory.monitorExecutorForVirtualThreads(
                                        new VirtualThreadExecutor("custom-tomcat-handler-"),
                                        "Http server pending request took:",
                                        "custom.http.server.pending"
                                )
                        )
        );
    }

//    @Override
//    public int getOrder() {
//        return 0 + 1;
//    }
}