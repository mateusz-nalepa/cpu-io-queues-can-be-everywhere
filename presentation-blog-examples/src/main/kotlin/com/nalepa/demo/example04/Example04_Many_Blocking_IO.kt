package com.nalepa.demo.example04

import java.time.LocalTime

fun main() {
    simulateBlockingIO(1)
    simulateBlockingIO(2)
    simulateBlockingIO(3)
}

fun simulateBlockingIO(index: Int) {
    println("")
    log("Start blocking IO code for: $index")
    Thread.sleep(5000) // http call, database call, reading from file, whatever what needs external data
    log("End blocking IO code for: $index")
}

fun log(message: String) {
    println("${Thread.currentThread().name} : ${LocalTime.now()} : $message")
}

