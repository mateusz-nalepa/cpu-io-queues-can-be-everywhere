package com.nalepa.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RequestSenderApp

fun main(args: Array<String>) {
    runApplication<RequestSenderApp>(*args)
}
