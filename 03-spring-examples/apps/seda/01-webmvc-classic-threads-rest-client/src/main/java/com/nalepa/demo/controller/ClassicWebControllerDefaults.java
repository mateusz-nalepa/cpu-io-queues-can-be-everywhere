package com.nalepa.demo.controller;

import com.nalepa.demo.common.Constants;
import com.nalepa.demo.common.DummyLogger;
import com.nalepa.demo.common.Operations;
import com.nalepa.demo.common.SomeResponse;
import com.nalepa.demo.httpclient.HttpClientFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@RestController
public class ClassicWebControllerDefaults {

    private final ObjectMapper objectMapper;
    private final org.springframework.web.client.RestClient restClient;

    public ClassicWebControllerDefaults(
            HttpClientFactory httpClientFactory,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.restClient = httpClientFactory.createRestClient();
    }

    @GetMapping("/endpoint/scenario/defaults/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    public ResponseEntity<SomeResponse> dummyEndpoint(
            @PathVariable String index,
            @PathVariable long mockDelaySeconds,
            @PathVariable long cpuOperationDelaySeconds
    ) {
        DummyLogger.log(this, "Start endpoint for index: " + index);

        try {
            byte[] byteArray = getData(index, mockDelaySeconds);
            SomeResponse someResponse = objectMapper.readValue(byteArray, SomeResponse.class);
            Operations.heavyCpuCode(cpuOperationDelaySeconds);
            return ResponseEntity.ok(someResponse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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