package com.nalepa.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Bulkhead_01_WebMVC_PlatformThreads

fun main(args: Array<String>) {
	runApplication<Bulkhead_01_WebMVC_PlatformThreads>(*args)
}
