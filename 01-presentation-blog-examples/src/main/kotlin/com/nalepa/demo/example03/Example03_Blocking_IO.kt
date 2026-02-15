package com.nalepa.demo.example03

import java.time.LocalTime

fun main() {
    simulateBlockingIO()
}

fun simulateBlockingIO() {
    log("Start blocking IO code")
    Thread.sleep(5000) // http call, database call, reading from file, whatever what needs external data
    log("End blocking IO code")
}

fun log(message: String) {
    println("${Thread.currentThread().name} : ${LocalTime.now()} : $message")
}
