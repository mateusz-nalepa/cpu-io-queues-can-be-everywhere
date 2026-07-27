package com.nalepa.demo.controller;

import com.nalepa.demo.common.DummyLogger;
import com.nalepa.demo.common.Operations;
import com.nalepa.demo.common.SomeResponse;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;

@RestController
public class NettyWebfluxControllerDefaults {

    private final ObjectMapper objectMapper;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public NettyWebfluxControllerDefaults(
            ReactiveMongoTemplate reactiveMongoTemplate,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    @GetMapping("/endpoint/scenario/defaults/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    public Mono<ResponseEntity<SomeResponse>> dummyEndpoint(
            @PathVariable String index,
            @PathVariable long mockDelaySeconds,
            @PathVariable long cpuOperationDelaySeconds
    ) {
        DummyLogger.log(this, "Start endpoint for index: " + index);

        return getData(index, mockDelaySeconds)
                .map(byteArray -> {
                    try {
                        Operations.heavyCpuCode(cpuOperationDelaySeconds);
                        return new SomeResponse("text");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(ResponseEntity::ok);
    }

    private Mono<byte[]> getData(String index, long mockDelaySeconds) {
        long startTime = System.nanoTime();


        var command = "{ sleep: 1, secs: " + mockDelaySeconds + ", lock: 'none'}";
        System.out.println(command);

        return
                reactiveMongoTemplate.executeCommand(org.bson.Document.parse(command))
                        .map(doc -> Map.of("ok", doc.get("ok")))
                        .doOnNext(bytes -> {
                            Duration duration = Duration.ofNanos(System.nanoTime() - startTime);
                            DummyLogger.log(this, "Index: " + index + ". Got response from webClient after: " + duration);
                        })
                        .map(document -> new byte[]{1, 2, 3});
    }
}