package com.nalepa.demo.controller;

import com.nalepa.demo.httpclient.HttpClientFactory;
import com.nalepa.demo.utils.ActionRecorder;
import com.nalepa.demo.utils.RequestSenderConfig;
import com.nalepa.demo.utils.RequestSenderLogger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.nalepa.demo.utils.SimulationStartedText.SIMULATION_STARTED_TEXT;

@RestController
public class SenderBulkheadEndpoint {

    private final ActionRecorder actionRecorder;
    private final RestClient restClient;

    private final ExecutorService executorForSlow = Executors.newFixedThreadPool(200);
    private final ExecutorService executorForFast = Executors.newFixedThreadPool(200);

    private Thread thread = new Thread(() -> {
    });

    public SenderBulkheadEndpoint(
            HttpClientFactory httpClientFactory,
            ActionRecorder actionRecorder
    ) {
        this.actionRecorder = actionRecorder;
        this.restClient = httpClientFactory.createRestClient();
    }

    @GetMapping("/send-requests-on-different-endpoints/scenario/{scenarioType}")
    public String simulateFlowRequestsOnDifferentEndpoints(
            @PathVariable String scenarioType,
            @RequestParam(required = false) Integer batchSize
    ) {
        RequestSenderConfig requestSenderConfig =
                batchSize == null
                        ? RequestSenderConfig.CpuCountRequestConfig.INSTANCE
                        : new RequestSenderConfig.UserProvidedRequestConfig(batchSize);

        thread = new Thread(() -> executeSimulation(scenarioType, requestSenderConfig));
        thread.start();

        return SIMULATION_STARTED_TEXT;
    }

    private void executeSimulation(String scenarioType, RequestSenderConfig requestSenderConfig) {
        RequestSenderLogger.log(this, "Start simulation for different endpoints on scenario: " + scenarioType);
        RequestSenderLogger.log(this, "Warmup");
        sendRequestForFastEndpoint(-1, scenarioType);

        while (true) {
            RequestSenderLogger.log(this, "\n\n");
            RequestSenderLogger.log(this, "Starting again for different endpoints on scenario: " + scenarioType + "!!");
            RequestSenderLogger.log(this, "Config: " + requestSenderConfig.numberOfRequestInBatch());

            List<Future<?>> futures = new ArrayList<>();

            RequestSenderLogger.log(this, "Send requests for SLOW endpoint");
            for (int index = 0; index < requestSenderConfig.numberOfRequestInBatch(); index++) {
                final int i = index;
                futures.add(executorForSlow.submit(() -> sendRequestForSlowEndpoint(i, 10, scenarioType)));
            }

            RequestSenderLogger.log(this, "Wait 2 seconds");
            try {
                Thread.sleep(Duration.ofSeconds(2));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            RequestSenderLogger.log(this, "Send requests for FAST endpoint");
            for (int index = requestSenderConfig.numberOfRequestInBatch(); index < requestSenderConfig.numberOfRequestInBatch() * 2; index++) {
                final int i = index;
                futures.add(executorForFast.submit(() -> sendRequestForFastEndpoint(i, scenarioType)));
            }

            RequestSenderLogger.log(this, "Wait until futures are finished");
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            RequestSenderLogger.log(this, "Futures are finished!");
        }
    }

    private void sendRequestForSlowEndpoint(int index, int appCpuOperationDelaySeconds, String scenarioType) {
        actionRecorder.record("slow", () ->
                restClient
                        .get()
                        .uri(
                                "http://localhost:8081/endpoint/scenario/{scenarioType}/slow/slow-{index}/{appCpuOperationDelaySeconds}",
                                scenarioType,
                                index,
                                appCpuOperationDelaySeconds
                        )
                        .retrieve()
                        .body(String.class)
        );
    }

    private void sendRequestForFastEndpoint(int index, String scenarioType) {
        actionRecorder.record("fast", () ->
                restClient
                        .get()
                        .uri("http://localhost:8081/endpoint/scenario/{scenarioType}/fast/fast-{index}", scenarioType, index)
                        .retrieve()
                        .body(String.class)
        );
    }
}