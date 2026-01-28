import com.nalepa.demo.common.monitored.ExecutorsFactory
import org.apache.coyote.ProtocolHandler
import org.apache.tomcat.util.threads.VirtualThreadExecutor
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading
import org.springframework.boot.thread.Threading
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.stereotype.Component

@Component
@ConditionalOnThreading(Threading.VIRTUAL)
class CustomTomcatVirtualThreadsWebServerCustomizer(
    private val executorsFactory: ExecutorsFactory,
) : WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> {

    // modified code from: TomcatVirtualThreadsWebServerFactoryCustomizer
    override fun customize(factory: ConfigurableTomcatWebServerFactory) {
        factory.addProtocolHandlerCustomizers(
            TomcatProtocolHandlerCustomizer { protocolHandler: ProtocolHandler ->
                protocolHandler.executor =
                    executorsFactory.monitorExecutor(
                        VirtualThreadExecutor("custom-tomcat-handler-"),
                        "Http server pending request took:",
                        "custom.http.server.pending"
                    )

            })
    }

//    override fun getOrder(): Int {
//        return 0 + 1
//    }


}