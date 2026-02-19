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

import java.util.concurrent.ExecutorService;

@RestController
public class VirtualWebControllerWithDedicatedCpuPool {

    private final ExecutorService executor;

    public VirtualWebControllerWithDedicatedCpuPool(ExecutorsFactory executorsFactory) {
        this.executor = executorsFactory.create(
                "CPU Bound pool waiting time took:",
                "CPU.for.slow",
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors()
        );
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/fast/{index}")
    public ResponseEntity<SomeResponse> fastEndpointDedicatedCpuPool(
            @PathVariable String index
    ) {
        DummyLogger.log(this, "Start FAST endpoint for index: " + index);

        return ResponseEntity.ok(new SomeResponse("fast"));
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/slow/{index}/{cpuOperationDelaySeconds}")
    public ResponseEntity<SomeResponse> slowEndpointDedicatedCpuPool(
            @PathVariable String index,
            @PathVariable long cpuOperationDelaySeconds
    ) {
        DummyLogger.log(this, "Start SLOW endpoint for index: " + index);

        executeHeavyCpuOperation(cpuOperationDelaySeconds);

        return ResponseEntity.ok(new SomeResponse("fast"));
    }

    private void executeHeavyCpuOperation(long cpuOperationDelaySeconds) {
        AsyncUtils.nonBlockingGet(
                executor.submit(() -> Operations.heavyCpuCode(cpuOperationDelaySeconds))
        );
    }
}