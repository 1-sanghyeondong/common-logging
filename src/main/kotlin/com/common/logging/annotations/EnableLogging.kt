package com.common.logging.annotations

import com.common.logging.common.StatusLoggingConfiguration
import com.common.logging.requestmapping.RequestMappingLoggerConfiguration
import org.springframework.context.annotation.Import

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(StatusLoggingConfiguration::class, RequestMappingLoggerConfiguration::class)
annotation class EnableLogging
