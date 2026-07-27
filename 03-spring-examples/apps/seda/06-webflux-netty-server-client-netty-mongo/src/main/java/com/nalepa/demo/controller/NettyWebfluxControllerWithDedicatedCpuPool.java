package com.nalepa.demo.controller;

import com.nalepa.demo.common.DummyLogger;
import com.nalepa.demo.common.Operations;
import com.nalepa.demo.common.SomeResponse;
import com.nalepa.demo.common.monitored.ExecutorsFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;

@RestController
public class NettyWebfluxControllerWithDedicatedCpuPool {

    private final ObjectMapper objectMapper;
    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private final Scheduler afterWebClientScheduler;


    public NettyWebfluxControllerWithDedicatedCpuPool(
            ExecutorsFactory executorsFactory,
            ReactiveMongoTemplate reactiveMongoTemplate,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.reactiveMongoTemplate = reactiveMongoTemplate;
        this.afterWebClientScheduler = Schedulers.fromExecutor(
                executorsFactory.create(
                        ExecutorsFactory.ThreadPoolConfig.builder()
                                .threadPoolName("cpu.pool")
                                .threadsSize(Runtime.getRuntime().availableProcessors())
                                // queue size can be high,
                                // because when CPU is bottleneck, it's better to wait in queue
                                // than process all requests at the same time
                                .taskQueueSize(400)
                                .build()
                )
        );
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    public Mono<ResponseEntity<SomeResponse>> dummyEndpoint(
            @PathVariable String index,
            @PathVariable long mockDelaySeconds,
            @PathVariable long cpuOperationDelaySeconds
    ) {
        DummyLogger.log(this, "Start endpoint for index: " + index);

        return Mono.just(index)
                // .map {  } // eq: deserialize requestBody, remember about validation if needed
                .flatMap(i -> getData(index, mockDelaySeconds))
                .publishOn(afterWebClientScheduler)
                .map(byteArray -> {
                    try {
//                        SomeResponse someResponse = objectMapper.readValue(byteArray, SomeResponse.class);
                        Operations.heavyCpuCode(cpuOperationDelaySeconds);
                        return new SomeResponse("123");
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

        return reactiveMongoTemplate
                .executeCommand(org.bson.Document.parse(command))
                .map(doc -> Map.of("ok", doc.get("ok")))
                .doOnNext(bytes -> {
                    Duration duration = Duration.ofNanos(System.nanoTime() - startTime);
                    DummyLogger.log(this, "Index: " + index + ". Got response from webClient after: " + duration);
                })
                .map(document -> new byte[]{1, 2, 3});
    }

}