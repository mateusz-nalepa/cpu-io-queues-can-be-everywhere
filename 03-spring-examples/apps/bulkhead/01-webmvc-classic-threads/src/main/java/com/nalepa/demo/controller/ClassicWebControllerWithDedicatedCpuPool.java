package com.nalepa.demo.controller;

import com.nalepa.demo.common.AsyncUtils;
import com.nalepa.demo.common.DummyLogger;
import com.nalepa.demo.common.Operations;
import com.nalepa.demo.common.SomeResponse;
import com.nalepa.demo.common.monitored.ExecutorsFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@RestController
public class ClassicWebControllerWithDedicatedCpuPool {

    private final ExecutorService executorForFast;
    private final ExecutorService executorForSlow;

    public ClassicWebControllerWithDedicatedCpuPool(ExecutorsFactory executorsFactory) {
        this.executorForFast = executorsFactory.create(
                "CPU Bound pool for fast waiting time took:",
                "CPU.for.fast",
                200,
                200
        );

        this.executorForSlow = executorsFactory.create(
                "CPU Bound pool for slow waiting time took:",
                "CPU.for.slow",
                200,
                200
        );
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/fast/{index}")
    public CompletableFuture<ResponseEntity<SomeResponse>> fastEndpointDedicatedCpuPool(
            @PathVariable String index
    ) {
        DummyLogger.log(this, "Start FAST endpoint for index: " + index);

        return AsyncUtils.async(executorForFast, () ->
                ResponseEntity.ok(new SomeResponse("slow"))
        );
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/slow/{index}/{cpuAppOrSleep}")
    public CompletableFuture<ResponseEntity<SomeResponse>> slowEndpointDedicatedCpuPool(
            @PathVariable String index,
            @PathVariable long cpuAppOrSleep
    ) {
        DummyLogger.log(this, "Start SLOW endpoint for index: " + index);

        return AsyncUtils.async(executorForSlow, () -> {
            try {
                // NOTE: in real app remember for example about: deserialize requestBody, remember about validation
                Operations.someBlockingIO(cpuAppOrSleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            return ResponseEntity.ok(new SomeResponse("fast"));
        });
    }
}