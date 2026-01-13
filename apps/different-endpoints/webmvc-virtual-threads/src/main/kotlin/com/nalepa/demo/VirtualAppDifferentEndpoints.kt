package com.nalepa.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VirtualAppDifferentEndpoints

fun main(args: Array<String>) {
    runApplication<VirtualAppDifferentEndpoints>(*args)
}
