package com.nalepa.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalTime;

@RestController
public class MockExternalServiceEndpoint {

    public record SomeResponse(String text) {}

    @GetMapping("/mock/{index}/{delaySeconds}")
    public ResponseEntity<SomeResponse> endpoint(
            @PathVariable String index,
            @PathVariable long delaySeconds
    ) {
        ResponseEntity<SomeResponse> response = ResponseEntity.ok(new SomeResponse("text"));
        nonBlockingSleep(delaySeconds);
        log(this, "Index: " + index + ". Response returned after seconds: " + delaySeconds);
        return response;
    }

    private static void log(Object caller, String message) {
        System.out.println(LocalTime.now() + " : " + caller.getClass().getSimpleName() + " : " + Thread.currentThread().getName() + " ### " + message);
    }

    private static void nonBlockingSleep(long seconds) {
        if (Thread.currentThread().isVirtual()) {
            try {
                Thread.sleep(Duration.ofSeconds(seconds));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("nonBlockingSleep called from carrier thread");
        }
    }
}