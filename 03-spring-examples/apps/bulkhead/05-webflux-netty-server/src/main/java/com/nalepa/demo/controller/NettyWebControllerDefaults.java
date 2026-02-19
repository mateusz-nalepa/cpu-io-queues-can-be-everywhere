package com.nalepa.demo.controller;

import com.nalepa.demo.common.DummyLogger;
import com.nalepa.demo.common.Operations;
import com.nalepa.demo.common.SomeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class NettyWebControllerDefaults {

    @GetMapping("/endpoint/scenario/defaults/fast/{index}")
    public Mono<ResponseEntity<SomeResponse>> fastEndpointDefaults(
            @PathVariable String index
    ) {
        DummyLogger.log(this, "Start FAST endpoint for index: " + index);

        return Mono.just(ResponseEntity.ok(new SomeResponse("fast")));
    }

    @GetMapping("/endpoint/scenario/defaults/slow/{index}/{cpuOperationDelaySeconds}")
    public Mono<ResponseEntity<SomeResponse>> slowEndpointDefaults(
            @PathVariable String index,
            @PathVariable long cpuOperationDelaySeconds
    ) {
        DummyLogger.log(this, "Start SLOW endpoint for index: " + index);

        Operations.heavyCpuCode(cpuOperationDelaySeconds);

        return Mono.just(ResponseEntity.ok(new SomeResponse("fast")));
    }
}