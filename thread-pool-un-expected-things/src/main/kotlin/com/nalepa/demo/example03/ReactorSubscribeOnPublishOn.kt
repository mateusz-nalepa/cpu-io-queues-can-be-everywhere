package com.nalepa.demo.example03

import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

fun main() {
//    anotherSubscribeOnHasNoEffect()
    subscribeOnThenPublishOn()
}

fun anotherSubscribeOnHasNoEffect() {
    val firstScheduler = Schedulers.newParallel("first-Scheduler")
    val secondScheduler = Schedulers.newParallel("second-Scheduler")

    Mono.just("value")
        .subscribeOn(firstScheduler)
        .doOnNext { println("${Thread.currentThread()} : first-Value") }

        .subscribeOn(secondScheduler)
        .doOnNext { println("${Thread.currentThread()} : second-Value") }
        .block()

    firstScheduler.dispose()
    secondScheduler.dispose()
}

fun subscribeOnThenPublishOn() {
    val firstScheduler = Schedulers.newParallel("first-Scheduler")
    val secondScheduler = Schedulers.newParallel("second-Scheduler")

    Mono.just("value")
        .subscribeOn(firstScheduler)
        .doOnNext { println("${Thread.currentThread()} : first-Value") }

        .publishOn(secondScheduler)
        .doOnNext { println("${Thread.currentThread()} : second-Value") }
        .block()

    firstScheduler.dispose()
    secondScheduler.dispose()
}






