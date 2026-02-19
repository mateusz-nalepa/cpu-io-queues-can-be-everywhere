package com.nalepa.demo.controller;

import com.nalepa.demo.common.DummyLogger;
import com.nalepa.demo.common.Operations;
import com.nalepa.demo.common.SomeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClassicWebControllerDefaults {

    @GetMapping("/endpoint/scenario/defaults/fast/{index}")
    public ResponseEntity<SomeResponse> fastEndpointDefaults(
            @PathVariable String index
    ) {
        DummyLogger.log(this, "Start FAST endpoint for index: " + index);

        return ResponseEntity.ok(new SomeResponse("fast"));
    }

    @GetMapping("/endpoint/scenario/defaults/slow/{index}/{cpuAppOrSleep}")
    public ResponseEntity<SomeResponse> slowEndpointDefaults(
            @PathVariable String index,
            @PathVariable long cpuAppOrSleep
    ) throws InterruptedException {
        DummyLogger.log(this, "Start SLOW endpoint for index: " + index);

        Operations.someBlockingIO(cpuAppOrSleep);

        return ResponseEntity.ok(new SomeResponse("fast"));
    }
}