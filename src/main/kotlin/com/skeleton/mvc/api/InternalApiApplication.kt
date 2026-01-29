package com.skeleton.mvc.api

import com.skeleton.mvc.api.common.logback.annotations.EnableLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableLogging
@SpringBootApplication
class InternalApiApplication

fun main(args: Array<String>) {
    runApplication<InternalApiApplication>(*args)
}
