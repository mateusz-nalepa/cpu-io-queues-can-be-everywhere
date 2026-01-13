package com.nalepa.demo.endpoints

import com.nalepa.demo.httpclient.HttpClientFactory
import com.nalepa.demo.utils.ActionRecorder
import com.nalepa.demo.utils.CpuCountRequestConfig
import com.nalepa.demo.utils.RequestSenderLogger
import com.nalepa.demo.utils.SIMULATION_STARTED_TEXT
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

    private val executorForFirstBatch = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val executorForSecondBatch = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    private var thread: Thread = Thread {}

    @GetMapping("/send-requests-app-with-client/scenario/{scenarioType}")
    fun simulateDefaultBehaviour(
        @PathVariable scenarioType: String,
    ): String {
        thread = Thread { executeSimulation(scenarioType) }
        thread.start()

        return SIMULATION_STARTED_TEXT
    }

    private fun executeSimulation(scenarioType: String) {
        RequestSenderLogger.log(this, "Start simulation for app with httpClient on scenarioType: $scenarioType")
        RequestSenderLogger.log(this, "Warmup")
        sendRequest("warmup", "-1", 0, 0, scenarioType)

        while (true) {

            RequestSenderLogger.log(this, "\n\n")
            RequestSenderLogger.log(this, "Starting again for app with httpClient on scenarioType: $scenarioType!!")
            RequestSenderLogger.log(this, "Config: ${CpuCountRequestConfig.numberOfRequestInBatch()}")

            val futures = mutableListOf<Future<*>>()

            RequestSenderLogger.log(this, "Send first batch of requests")

            (0..<CpuCountRequestConfig.numberOfRequestInBatch()).forEach { index ->
                executorForFirstBatch
                    .submit { sendRequest("firstBatch", "firstBatch-${index}", 0, 10, scenarioType) }
                    .let { futures.add(it) }
            }

            RequestSenderLogger.log(this, "Wait 2 seconds")
            Thread.sleep(Duration.ofSeconds(2))

            RequestSenderLogger.log(this, "Send second batch of requests")
            (CpuCountRequestConfig.numberOfRequestInBatch()..<CpuCountRequestConfig.numberOfRequestInBatch() * 2).forEach { index ->
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



