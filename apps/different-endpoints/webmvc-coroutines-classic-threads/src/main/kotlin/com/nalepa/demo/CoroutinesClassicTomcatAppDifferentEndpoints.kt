package com.nalepa.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * There is no
 */
@SpringBootApplication
class CoroutinesClassicTomcatAppDifferentEndpoints

fun main(args: Array<String>) {
	runApplication<CoroutinesClassicTomcatAppDifferentEndpoints>(*args)
}
