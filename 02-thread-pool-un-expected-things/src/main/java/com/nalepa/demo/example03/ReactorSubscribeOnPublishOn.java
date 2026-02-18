package com.nalepa.demo.example03;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class ReactorSubscribeOnPublishOn {
    public static void main(String[] args) {
         anotherSubscribeOnHasNoEffect();
//        subscribeOnThenPublishOn();
    }

    static void anotherSubscribeOnHasNoEffect() {
        Scheduler firstScheduler = Schedulers.newParallel("first-Scheduler");
        Scheduler secondScheduler = Schedulers.newParallel("second-Scheduler");

        Mono.just("value")
            .subscribeOn(firstScheduler)
            .doOnNext(v -> System.out.println(Thread.currentThread() + " : first-Value"))
            .subscribeOn(secondScheduler)
            .doOnNext(v -> System.out.println(Thread.currentThread() + " : second-Value"))
            .block();

        firstScheduler.dispose();
        secondScheduler.dispose();
    }

    static void subscribeOnThenPublishOn() {
        Scheduler firstScheduler = Schedulers.newParallel("first-Scheduler");
        Scheduler secondScheduler = Schedulers.newParallel("second-Scheduler");

        Mono.just("value")
            .subscribeOn(firstScheduler)
            .doOnNext(v -> System.out.println(Thread.currentThread() + " : first-Value"))
            .publishOn(secondScheduler)
            .doOnNext(v -> System.out.println(Thread.currentThread() + " : second-Value"))
            .block();

        firstScheduler.dispose();
        secondScheduler.dispose();
    }

    // Przykład analogiczny do kotlina:
    // val someScheduler = Schedulers.newBoundedElastic(10, 100, "some-scheduler")
    // private fun getDummyData(index: String, mockDelaySeconds: Long): Mono<ByteArray> { ... }


}
