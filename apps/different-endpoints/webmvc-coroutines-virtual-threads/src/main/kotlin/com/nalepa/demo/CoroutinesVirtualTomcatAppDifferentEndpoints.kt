package com.nalepa.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * There is no
 */
@SpringBootApplication
class CoroutinesVirtualTomcatAppDifferentEndpoints

fun main(args: Array<String>) {
	runApplication<CoroutinesVirtualTomcatAppDifferentEndpoints>(*args)
}
