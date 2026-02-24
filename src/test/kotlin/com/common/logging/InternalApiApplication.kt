package com.common.logging

import com.common.logging.annotations.EnableLogging
import com.common.logging.annotations.EnableMDCTraceLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableLogging
@EnableMDCTraceLogging
@SpringBootApplication
class InternalApiApplication

fun main(args: Array<String>) {
    runApplication<InternalApiApplication>(*args)
}
