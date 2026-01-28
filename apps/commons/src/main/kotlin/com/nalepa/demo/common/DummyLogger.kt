package com.nalepa.demo.common

object DummyLogger {
    fun log(caller: Any, message: String) {
//        println("${LocalTime.now()} : ${caller.javaClass.simpleName} : ${Thread.currentThread().name} \n\t\t$message")
    }
}