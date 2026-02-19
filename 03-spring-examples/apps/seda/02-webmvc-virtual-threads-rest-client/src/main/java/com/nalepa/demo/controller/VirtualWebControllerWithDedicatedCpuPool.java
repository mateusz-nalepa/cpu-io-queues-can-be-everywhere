package com.nalepa.demo.controller;

import com.nalepa.demo.common.AsyncUtils;
import com.nalepa.demo.common.Constants;
import com.nalepa.demo.common.DummyLogger;
import com.nalepa.demo.common.Operations;
import com.nalepa.demo.common.SomeResponse;
import com.nalepa.demo.common.monitored.ExecutorsFactory;
import com.nalepa.demo.httpclient.HttpClientFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

@RestController
public class VirtualWebControllerWithDedicatedCpuPool {

    private final ObjectMapper objectMapper;
    private final org.springframework.web.client.RestClient restClient;
    private final ExecutorService cpuExecutor;

    public VirtualWebControllerWithDedicatedCpuPool(
            HttpClientFactory httpClientFactory,
            ObjectMapper objectMapper,
            ExecutorsFactory executorsFactory
    ) {
        this.objectMapper = objectMapper;
        this.restClient = httpClientFactory.createRestClient();
        this.cpuExecutor = executorsFactory.create(
                "CPU Bound pool waiting time took:",
                "CPU.executor",
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors()
        );
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    public ResponseEntity<SomeResponse> dummyEndpoint(
            @PathVariable String index,
            @PathVariable long mockDelaySeconds,
            @PathVariable long cpuOperationDelaySeconds
    ) {
        DummyLogger.log(this, "Start endpoint for index: " + index);

        // NOTE: if needed, deserialize request on another cpu pool, remember about validation if present
        byte[] byteArray = getData(index, mockDelaySeconds);
        SomeResponse someResponse = executeHeavyCpuOperation(byteArray, cpuOperationDelaySeconds);
        return ResponseEntity.ok(someResponse);
    }

    private SomeResponse executeHeavyCpuOperation(byte[] byteArray, long cpuOperationDelaySeconds) {
        return AsyncUtils.safeGetOnVirtual(
                cpuExecutor.submit(() -> {
                    try {
                        SomeResponse someResponse = objectMapper.readValue(byteArray, SomeResponse.class);
                        Operations.heavyCpuCode(cpuOperationDelaySeconds);
                        return someResponse;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }

    private byte[] getData(String index, long mockDelaySeconds) {
        long startTime = System.nanoTime();

        byte[] body = restClient
                .get()
                .uri("http://localhost:8082/mock/{index}/{mockDelaySeconds}", index, mockDelaySeconds)
                .header(Constants.DUMMY_INDEX, index)
                .retrieve()
                .body(byte[].class);

        Duration duration = Duration.ofNanos(System.nanoTime() - startTime);
        DummyLogger.log(this, "Index: " + index + ". Got response from restClient after: " + duration);

        return body;
    }
}