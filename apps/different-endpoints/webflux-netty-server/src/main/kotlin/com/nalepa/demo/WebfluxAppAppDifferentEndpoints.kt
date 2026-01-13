package com.nalepa.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WebfluxAppAppDifferentEndpoints

fun main(args: Array<String>) {
	runApplication<WebfluxAppAppDifferentEndpoints>(*args)
}
