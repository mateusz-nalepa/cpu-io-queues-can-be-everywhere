package com.nalepa.demo.controller;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * request
 *    -> IO Pool
 *    -> CPU Pool
 *    -> another IO Pool (if needed)
 *    -> another CPU Pool (if needed)
 */
@RestController
public class ClassicWebControllerDefaultsWithDedicatedCpuPool {

    private final ObjectMapper objectMapper;
    private final org.springframework.web.client.RestClient restClient;

    // IO-bound: hide latency, avoid queue wait time.
    // Context switching here is helpful — CPU usage for IO is basically 0%
    private final Executor ioExecutor;

    // CPU-bound: small queue is fine; high CPU usage should trigger scaling.
    // Context switching here is expensive — CPU is the bottleneck
    private final Executor cpuExecutor;

    public ClassicWebControllerDefaultsWithDedicatedCpuPool(
            HttpClientFactory httpClientFactory,
            ExecutorsFactory executorsFactory,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.restClient = httpClientFactory.createRestClient();

        this.ioExecutor = executorsFactory.create(
                "IO Bound Pool",
                "io",
                400,
                // in order to prevent microburst present in demonstration :D
                // in normal situation, this queue should not be so high, there should be more threads
                // app should wait for response from I/O, not sit down in queue here
                400
        );

        this.cpuExecutor = executorsFactory.create(
                "CPU Bound Pool",
                "cpu",
                // In normal conditions, set Runtime.getRuntime().availableProcessors()
                // 200 is only for demonstration purposes in this repo
                200,
                400
        );
    }

    @GetMapping("/endpoint/scenario/dedicatedCpuPool/{index}/{mockDelaySeconds}/{cpuOperationDelaySeconds}")
    public CompletableFuture<ResponseEntity<SomeResponse>> dummyEndpoint(
            @PathVariable String index,
            @PathVariable long mockDelaySeconds,
            @PathVariable long cpuOperationDelaySeconds
    ) {
        DummyLogger.log(this, "Start endpoint for index: " + index);

        return startAsyncOn(ioExecutor)
                .with(() -> getData(index, mockDelaySeconds))
                .publishOn(cpuExecutor)
                .map(byteArray -> {
                    try {
                        SomeResponse someResponse = objectMapper.readValue(byteArray, SomeResponse.class);
                        Operations.heavyCpuCode(cpuOperationDelaySeconds);
                        return someResponse;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(ResponseEntity::ok)
                .toCompletableFuture();
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

    // in order to have something like: SLR - Single Line Responsibility
    public static FluentFuture<Void> startAsyncOn(Executor executor) {
        return FluentFuture.of(CompletableFuture.runAsync(() -> {}, executor));
    }

    // -------------------------------------------------------------------------

    public static class FluentFuture<T> {

        private final CompletableFuture<T> future;

        private FluentFuture(CompletableFuture<T> future) {
            this.future = future;
        }

        public static <T> FluentFuture<T> of(CompletableFuture<T> future) {
            return new FluentFuture<>(future);
        }

        public <U> FluentFuture<U> with(Supplier<U> action) {
            return new FluentFuture<>(future.thenApply(ignored -> action.get()));
        }

        public FluentFuture<T> publishOn(Executor executor) {
            return new FluentFuture<>(future.thenApplyAsync(Function.identity(), executor));
        }

        public <U> FluentFuture<U> map(Function<T, U> action) {
            return new FluentFuture<>(future.thenApply(action));
        }

        public <U> FluentFuture<U> flatMap(Function<T, CompletableFuture<U>> action) {
            return new FluentFuture<>(future.thenCompose(action));
        }

        public CompletableFuture<T> toCompletableFuture() {
            return future;
        }
    }
}