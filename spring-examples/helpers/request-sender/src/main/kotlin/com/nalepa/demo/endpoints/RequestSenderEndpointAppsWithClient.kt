package com.nalepa.demo.endpoints

import com.nalepa.demo.httpclient.HttpClientFactory
import com.nalepa.demo.utils.ActionRecorder
import com.nalepa.demo.utils.CpuCountRequestConfig
import com.nalepa.demo.utils.RequestSenderConfig
import com.nalepa.demo.utils.RequestSenderLogger
import com.nalepa.demo.utils.SIMULATION_STARTED_TEXT
import com.nalepa.demo.utils.UserProvidedRequestConfig
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future

@RestController
class RequestSenderEndpointAppsWithClient(
    private val httpClientFactory: HttpClientFactory,
    private val actionRecorder: ActionRecorder,
) {

    private val restClient = httpClientFactory.createRestClient()

    private val executorForFirstBatch = Executors.newFixedThreadPool(200)
    private val executorForSecondBatch = Executors.newFixedThreadPool(200)

    private var thread: Thread = Thread {}

    @GetMapping("/send-requests-app-with-client/scenario/{scenarioType}")
    fun simulateDefaultBehaviour(
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

    private fun executeSimulation(scenarioType: String, requestSenderConfig: RequestSenderConfig) {
        RequestSenderLogger.log(this, "Start simulation for app with httpClient on scenarioType: $scenarioType")
        RequestSenderLogger.log(this, "Config: ${requestSenderConfig.numberOfRequestInBatch()}")

        RequestSenderLogger.log(this, "Warmup")
        sendRequest("warmup", "-1", 0, 0, scenarioType)

        while (true) {

            RequestSenderLogger.log(this, "\n\n")
            RequestSenderLogger.log(this, "Starting again for app with httpClient on scenarioType: $scenarioType!!")
            RequestSenderLogger.log(this, "Config: ${requestSenderConfig.numberOfRequestInBatch()}")

            val futures = mutableListOf<Future<*>>()

            RequestSenderLogger.log(this, "Send first batch of requests")

            (0..<requestSenderConfig.numberOfRequestInBatch()).forEach { index ->
                executorForFirstBatch
                    .submit { sendRequest("firstBatch", "firstBatch-${index}", 0, 10, scenarioType) }
                    .let { futures.add(it) }
            }

            RequestSenderLogger.log(this, "Wait 2 seconds")
            Thread.sleep(Duration.ofSeconds(2))

            RequestSenderLogger.log(this, "Send second batch of requests")
            (requestSenderConfig.numberOfRequestInBatch()..<requestSenderConfig.numberOfRequestInBatch() * 2).forEach { index ->
                executorForSecondBatch
                    .submit { sendRequest("secondBatch", "secondBatch-${index}", 9, 10, scenarioType) }
                    .let { futures.add(it) }
            }

            RequestSenderLogger.log(this, "Wait until futures are finished")
            futures.forEach { it.get() }
            RequestSenderLogger.log(this, "Futures are finished!")

        }
    }

    private fun sendRequest(
        batchName: String,
        index: String,
        mockDelaySeconds: Int,
        appCpuOperationDelaySeconds: Int,
        scenarioType: String
    ) {
        actionRecorder.record(batchName) {
            restClient
                .get()
                .uri(
                    "http://localhost:8081/endpoint/scenario/{scenarioType}/{index}/{mockDelaySeconds}/{appCpuOperationDelaySeconds}",
                    scenarioType,
                    index,
                    mockDelaySeconds,
                    appCpuOperationDelaySeconds
                )
                .retrieve()
                .body(String::class.java)
        }
    }

}



