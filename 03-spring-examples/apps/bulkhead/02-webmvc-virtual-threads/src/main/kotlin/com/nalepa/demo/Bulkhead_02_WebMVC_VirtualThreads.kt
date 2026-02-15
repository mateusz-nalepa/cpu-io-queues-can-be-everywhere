package com.nalepa.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Bulkhead_02_WebMVC_VirtualThreads

fun main(args: Array<String>) {
    runApplication<Bulkhead_02_WebMVC_VirtualThreads>(*args)
}
