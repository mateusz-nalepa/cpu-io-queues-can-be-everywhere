package com.nalepa.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VirtualAppWithHttpClient

fun main(args: Array<String>) {
	runApplication<VirtualAppWithHttpClient>(*args)
}
