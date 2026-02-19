package com.nalepa.demo.controller;

import com.nalepa.demo.common.DummyLogger;
import com.nalepa.demo.common.Operations;
import com.nalepa.demo.common.SomeResponse;
import com.nalepa.demo.common.monitored.ExecutorsFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@RestController
public class NettyWebControllerWithDedicatedCpuPool {

    private final Scheduler schedulerForFast;
    private final Scheduler schedulerForSlow;

    public NettyWebControllerWithDedicatedCpuPool(ExecutorsFactory executorsFactory) {
        // thanks to those executors, server threads are like I/O threads
        this.schedulerForFast = Schedulers.fromExecutor(
                executorsFactory.create(
                        "CPU Bound pool waiting time took:",
                        "CPU.for.fast",
                        Runtime.getRuntime().availableProcessors(),
                        Runtime.getRuntime().availableProcessors()
                )
        );

        // in theory this is not needed, in practice in reactor I have no idea how to monitor tasks queue wait time for server
        this.schedulerForSlow = Schedulers.fromExecutor(
                executorsFactory.create(
                        "CPU Bound pool waiting time took:",
                        "CPU.for.slow",
                        Runtime.getRuntime().availableProcessors(),
                        Runtime.getRuntime().availableProcessors()
                )
        );
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/fast/{index}")
    public Mono<ResponseEntity<SomeResponse>> fastEndpointDedicatedCpuPool(
            @PathVariable String index
    ) {
        DummyLogger.log(this, "Start FAST endpoint for index: " + index);

        return Mono
                .just(index)
                .publishOn(schedulerForFast)
                .map(i -> {
                    // some processing here
                    return ResponseEntity.ok(new SomeResponse("fast"));
                });
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/slow/{index}/{cpuOperationDelaySeconds}")
    public Mono<ResponseEntity<SomeResponse>> slowEndpointDedicatedCpuPool(
            @PathVariable String index,
            @PathVariable long cpuOperationDelaySeconds
    ) {
        DummyLogger.log(this, "Start SLOW endpoint for index: " + index);

        return Mono
                .just(index)
                .publishOn(schedulerForSlow)
                .doOnNext(i -> Operations.heavyCpuCode(cpuOperationDelaySeconds))
                .map(i -> {
                    // some processing here
                    return ResponseEntity.ok(new SomeResponse("slow"));
                });
    }
}