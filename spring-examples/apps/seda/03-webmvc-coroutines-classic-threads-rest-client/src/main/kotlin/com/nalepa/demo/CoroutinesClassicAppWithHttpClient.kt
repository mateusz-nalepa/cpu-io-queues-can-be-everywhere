package com.nalepa.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CoroutinesClassicAppWithHttpClient

fun main(args: Array<String>) {
	runApplication<CoroutinesClassicAppWithHttpClient>(*args)
}
