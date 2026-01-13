package com.nalepa.demo.endpoints

import com.nalepa.demo.httpclient.HttpClientFactory
import com.nalepa.demo.utils.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future


@RestController
class RequestSenderDifferentEndpoint(
    private val httpClientFactory: HttpClientFactory,
    private val actionRecorder: ActionRecorder,
) {
    private val restClient = httpClientFactory.createRestClient()

    private val executorForSlow = Executors.newFixedThreadPool(200)
    private val executorForFast = Executors.newFixedThreadPool(200)

    private var thread: Thread = Thread {}

    @GetMapping("/send-requests-on-different-endpoints/scenario/{scenarioType}")
    fun simulateFlowRequestsOnDifferentEndpoints(
        @PathVariable scenarioType: String,
        @RequestParam(required = false) batchSize: Int?,
    ): String {

        val requestSenderConfig =
            if (batchSize == null) {
                CpuCountRequestConfig
            } else {
                UserProvidedRequestConfig(batchSize)
            }

        thread = Thread { executeSimulation(scenarioType, requestSenderConfig) }
        thread.start()


        return SIMULATION_STARTED_TEXT
    }

    private fun executeSimulation(
        scenarioType: String,
        requestSenderConfig: RequestSenderConfig
    ) {

        RequestSenderLogger.log(this, "Start simulation for different endpoints on scenario: $scenarioType")
        RequestSenderLogger.log(this, "Warmup")
        sendRequestForFastEndpoint(-1, scenarioType)

        while (true) {
            RequestSenderLogger.log(this, "\n\n")
            RequestSenderLogger.log(this, "Starting again for different endpoints on scenario: $scenarioType!!")
            RequestSenderLogger.log(this, "Config: ${requestSenderConfig.numberOfRequestInBatch()}")
            val futures = mutableListOf<Future<*>>()

            RequestSenderLogger.log(this, "Send requests for SLOW endpoint")

            (0..<requestSenderConfig.numberOfRequestInBatch()).forEach { index ->
                executorForSlow
                    .submit { sendRequestForSlowEndpoint(index, 10, scenarioType) }
                    .let { futures.add(it) }
            }

            RequestSenderLogger.log(this, "Wait 2 seconds")
            Thread.sleep(Duration.ofSeconds(2))

            RequestSenderLogger.log(this, "Send requests for FAST endpoint")
            (requestSenderConfig.numberOfRequestInBatch()..<requestSenderConfig.numberOfRequestInBatch() * 2).forEach { index ->
                executorForFast
                    .submit { sendRequestForFastEndpoint(index, scenarioType) }
                    .let { futures.add(it) }
            }

            RequestSenderLogger.log(this, "Wait until futures are finished")
            futures.forEach { it.get() }
            RequestSenderLogger.log(this, "Futures are finished!")
        }


    }

    private fun sendRequestForSlowEndpoint(
        index: Int,
        appCpuOperationDelaySeconds: Int,
        scenarioType: String
    ) {
        actionRecorder.record("slow") {

            restClient
                .get()
                .uri(
                    "http://localhost:8081/endpoint/scenario/{scenarioTyp}/slow/slow-{index}/{appCpuOperationDelaySeconds}",
                    scenarioType,
                    index,
                    appCpuOperationDelaySeconds
                )
                .retrieve()
                .body(String::class.java)
        }
    }

    private fun sendRequestForFastEndpoint(
        index: Int,
        scenarioType: String
    ) {
        actionRecorder.record("fast") {
            restClient
                .get()
                .uri("http://localhost:8081/endpoint/scenario/{scenarioType}/fast/fast-{index}", scenarioType, index)
                .retrieve()
                .body(String::class.java)
        }

    }
}


